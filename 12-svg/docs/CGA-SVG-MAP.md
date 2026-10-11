# Kintsugi SVG ↔ CGA Asset Format (`/v1`)

Quick reference for the `/v1` SVG format: the `k:` namespace, the CGA-primitive → SVG-element
mapping table, and one example per shape. This is a standalone at-a-glance doc — the normative
rules (emission tiers, edge cases, parse/serialize contracts, and the rationale behind each) are
maintained alongside the implementation.

## Purpose

Maps Kintsugi's eight CGA shapes (five primitives + the `Segment`, `Polygon`, and `Polyline` composites — rectangles are `Polygon.Rectangle` factory quads, not a type) to and from SVG:

- **Lossless round-trip** CGA → SVG → CGA, where "lossless" means *canonical-equality /
  `Double`-idempotent* (re-parsing reproduces the same CGA values) — **not** byte-verbatim text.
  Cosmetic FP drift across save cycles (e.g. `255.79` → `255.78999999999996`) is normal, not a bug.
- **Designer-authored SVG** (Inkscape/Illustrator) loads without Kintsugi markup via natural
  defaults — a bare `<circle>` is a circle, a bare `<g>` is a group.
- **Standard renderer compatibility** on **SVG 1.1** (modern browsers, Inkscape).

## XML Namespace

Declared once on the `<svg>` root:

```xml
<svg xmlns="http://www.w3.org/2000/svg"
     xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1"
     viewBox="0 0 100 100">
```

- The namespace URI is an identifier, not a locator — it doesn't need to resolve. The `/v1` suffix
  leaves room for future breaking versions.
- All SVG coordinates use the standard SVG convention (origin top-left, Y-axis down) so any viewer
  renders correctly. Kintsugi applies the SVG → CGA mapping (centering + Y-flip) at load time.
- **SVG 1.1 target, two extensions.** Over plain SVG 1.1: the `k:` namespace for sidecar
  attributes, and external whole-document `<use href="doc.svg">` template references (parse-only;
  renders today only in served modern Chromium — see KVG_SPEC.md §Foundations for the support
  picture). No `<symbol>`, `<defs>`, or internal `#id` references.

## Consolidated Mapping

| CGA primitive | SVG element | Recognized by | Geometry / data carried |
|---|---|---|---|
| **Flat point** | `<rect>` | `k:type="flat-point"` | `width = height = size`, centered on the CGA position; `fill` |
| **Round point** | `<circle>` | `k:type="round-point"` | `r = size/2` (**render marker size, not radius²**); `cx`/`cy` = position; `az = 1` (unitization); `aw` from `k:aw` (default: `(x² + y²)/2` → null round point); `fill` |
| **Circle** | `<circle>` | element name (no `k:type`) | `cx`, `cy`, `r` = geometric radius `√\|r²\|`; `fill`/`stroke`/`stroke-width`; `k:flags` tokens `imaginary`, `inside-out` |
| **Unbounded line** | `<line>` | `k:type="line"` | `(x1,y1) → (x2,y2)`, order = orientation (`Line = FlatPoint(x1,y1) ∧ FlatPoint(x2,y2)`); endpoints recomputed at export from `blade ∩ worldBorder`; `stroke`/`stroke-width`; `stroke-linecap` (only when non-`butt`) |
| **Segment** | `<line>` | element name (no `k:type`) | `(x1,y1) → (x2,y2)`, order = orientation; `stroke`/`stroke-width`; `stroke-linecap` (only when non-`butt`) |
| **Rectangle (Polygon quad)** | `<rect>` **parse-only** → writes `<polygon>` | element name (no `k:type`) | `x`/`y`/`width`/`height` decode through `Polygon.Rectangle` into the fixed CCW quad bl → br → tr → tl; `k:flags` token `inside-out` reverses the walk (wall reading); `fill`/`stroke`/`stroke-width`; non-zero `rx`/`ry` and negative extents rejected |
| **Polygon** | `<polygon>` | element name (no `k:type`) | `points` = `x,y` pairs in traversal order (closes implicitly), canonicalized against the `k:flags` token `inside-out` — flagged loops decode clockwise (inside-out), unflagged counter-clockwise; `fill`/`stroke`/`stroke-width` |
| **Polyline** | `<polyline>` | element name (no `k:type`) | `points` = `x,y` pairs in traversal order (≥ 2 pairs; open chain — no implicit close; first == last legal); `fill`/`stroke`/`stroke-width`; `stroke-linecap` (only when non-`butt`) |
| **Dipole** | `<g k:type="dipole">` | `k:type` on `<g>` | two `<circle k:type="round-point">` children + an inline dashed `<line>`; child order = orientation; the `<line>` is visual chrome, ignored by the parser |
| **Group (GroupNode)** | `<g>` | element name (no `k:type`) | recursive children of any supported element; nested `bounds` is NOT persisted (only the root bounds round-trips) |
| **Template reference** | `<use>` **parse-only** → writes expanded shapes | element name | `href` = external template document (fragment tolerated, ignored); `x`/`y` place the template's top-left (origin anchoring); expands at parse via the caller's templates map — never re-serialized |

**Seven element types** (`<rect>`, `<circle>`, `<line>`, `<polygon>`, `<polyline>`, `<g>`, `<use>`), most doing double duty (`<polygon>`/`<polyline>` have no tagged reading — the bare element is always the composite):

- `<circle>` is a **round point** when tagged `k:type="round-point"`, otherwise a **circle** (the
  untagged default — designer-authored circles "just work").
- `<rect>` is a **flat point** when tagged `k:type="flat-point"`, otherwise a **rectangle Polygon**
  (the untagged default — designer-authored rects "just work"). `<rect>` is **parse-only**:
  rectangles re-serialize as `<polygon>` (lossy at the element level, lossless at the CGA level).
  There is no background reading — a scene backdrop is an ordinary full-bleed rectangle, first in
  z-order.
- `<g>` is a **dipole** when tagged `k:type="dipole"`, otherwise a **GroupNode** (the untagged
  default — designer-authored Inkscape groups "just work").

`k:type` discriminators in use: `flat-point`, `round-point`, `line`, `dipole`. Circles, segments,
rectangles, polygons, polylines, and GroupNodes are the untagged defaults; the serializer **omits**
`k:type` for them. Where present, `k:type` is the first attribute emitted. An unknown `k:type`
warns and falls back to the untagged default of its element (circle / segment / rectangle /
polygon / polyline / GroupNode).

**`k:tags`** — open-vocabulary space-separated game-semantic tokens (`ShapeNode.tags`), legal on
every shape element, `<g>` (unions into descendant leaves), and `<use>` (unions onto every
expanded leaf). Verbatim (no lowercasing), renderer-inert, emitted only when non-empty. Sibling
of `k:flags`, which stays a closed vocabulary.

## Examples

### Flat point

```xml
<!-- Flat point at (10, 5) with render size 8. CGA position = rect center; rect is square. -->
<rect k:type="flat-point" x="6" y="1" width="8" height="8" fill="#00ff00"/>
```

### Round point

`r` is the render marker size (`size/2`), *not* the CGA radius; the algebraic radius² rides in
`k:aw` (raw `e4` weight). Omitting `k:aw` means the null round point (`aw = (cx² + cy²)/2`).

```xml
<!-- Null round point at (10, 5), render marker size 8 (r = 4) -->
<circle k:type="round-point" cx="10" cy="5" r="4" fill="#00ff00"/>

<!-- Non-null round point, r² = 9: aw = (9 + 100 + 25) / 2 = 67 -->
<circle k:type="round-point" cx="10" cy="5" r="4" k:aw="67" fill="#00ff00"/>

<!-- Imaginary round point, r² = -4: aw = (-4 + 100 + 25) / 2 = 60.5 -->
<circle k:type="round-point" cx="10" cy="5" r="4" k:aw="60.5" fill="#00ff00"/>
```

### Circle

Here `r` *is* the geometric radius (`√|r²|`). Untagged `<circle>` is a CGA circle.

```xml
<!-- Real classic circle — the 99% case, zero k:* attributes -->
<circle cx="10" cy="5" r="3"/>

<!-- Imaginary (r² < 0), inside-out (cw > 0), or both -->
<circle cx="10" cy="5" r="3" k:flags="imaginary"/>
<circle cx="10" cy="5" r="3" k:flags="inside-out"/>
<circle cx="10" cy="5" r="3" k:flags="imaginary inside-out"/>
```

### Unbounded line

Endpoints are the worldBorder-clipped projection of the algebraic line; endpoint order encodes
the blade's orientation.

```xml
<line k:type="line" x1="0" y1="30" x2="100" y2="70"/>
```

### Segment

Bare `<line>` (no `k:type`) is the untagged default; decodes via `Segment(FlatPoint(x1,y1),
FlatPoint(x2,y2))`. Endpoints stored as given (no worldBorder clipping).

```xml
<line x1="0" y1="0" x2="10" y2="0" stroke="#00ff00" stroke-width="1"/>
```

### Rectangle (parse-only `<rect>`)

Bare `<rect>` (no `k:type`) is the untagged default: it decodes through `Polygon.Rectangle` into
the fixed CCW quad **bl → br → tr → tl** (the factory-order invariant — consumers may index
`edges[0]`=bottom … `[3]`=left). `k:flags="inside-out"` reverses the walk — the rect decodes as
an inside-out rectangle Polygon, the wall reading. There is no Rectangle type; the shape
**re-serializes as
`<polygon>`** (first save converts the element, subsequent saves are stable). Non-zero `rx`/`ry`
(rounded corners) and negative extents are rejected — no arcs in `/v1`, and an SVG extent error
stays an error. A full-bleed rect (matching the viewBox exactly) carries no special meaning —
it's how a scene backdrop is encoded, as the first child in z-order.

```xml
<!-- Reads fine in /v1 — zero k:* attributes; renders identically everywhere -->
<rect x="20" y="30" width="40" height="20" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
<!-- …and re-serializes as the equivalent quad (viewBox 0 0 100 100): -->
<polygon points="20,50 60,50 60,30 20,30" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
```

### Polygon

Bare `<polygon>` is the untagged (and only) reading. `points` pairs are the vertices in traversal
order and the loop closes implicitly. Coordinates go through the same Y-flip as every other
element. Orientation rides `k:flags="inside-out"` as intent, model winding as truth: the parsed
walk is reversed when it contradicts the flag — flagged loops decode clockwise
(`Polygon.isInsideOut`, the wall reading), unflagged loops counter-clockwise (classic) — and the
serializer emits the flag exactly when the polygon is inside-out, so saved documents re-parse
without any reversal.

```xml
<!-- Triangle — zero k:* attributes; renders identically everywhere -->
<polygon points="20,80 60,80 20,50" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
<!-- The same loop inside out — a wall whose solid region is everything outside it -->
<polygon points="20,50 60,80 20,80" k:flags="inside-out"/>
```

### Polyline

Bare `<polyline>` is the untagged (and only) reading: an open chain (≥ 2 vertex pairs, no implicit
close; first == last is legal and preserved as data). Unlike the closed polygon, the open stroke
honors `stroke-linecap` (emitted only when non-`butt`, like segment).

```xml
<!-- Open chain — zero k:* attributes; renders identically everywhere -->
<polyline points="20,80 60,80 20,50" fill="none" stroke="#0000ff" stroke-width="2"/>
```

### Dipole

The two `<circle k:type="round-point">` children carry the algebra; child order is the orientation
contract (first = left operand of `start ∧ end`). The dashed `<line>` is inline visual chrome and
is ignored by the parser.

```xml
<g k:type="dipole">
  <circle k:type="round-point" cx="0"  cy="0" r="4" fill="#00ff00"/>
  <circle k:type="round-point" cx="10" cy="0" r="4" fill="#00ff00"/>
  <line x1="0" y1="0" x2="10" y2="0" stroke-dasharray="2 1"/>
</g>
```

### Template reference (parse-only `<use>`) + `k:tags`

External whole-document reference: `href` names a template SVG (supplied to
`parseSvg(content, templates)` as raw text; `:game-engine`'s loader fetches siblings automatically),
`x`/`y` place its viewport top-left. The template must declare `width`/`height` equal to its
viewBox extents; `width`/`height` on `<use>` is rejected (no scaling). Use-site `fill` etc. and
`k:tags` flow into the template's *unset* slots; template tags union with use-site tags on every
expanded leaf. Expansion is parse-only — a save writes the placed shapes.

```xml
<!-- steel.svg — the template -->
<svg xmlns="http://www.w3.org/2000/svg"
     xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1"
     width="60" height="30" viewBox="0 0 60 30">
  <rect x="0" y="0" width="60" height="30" fill="#708090" k:tags="brick steel"/>
</svg>

<!-- wall.svg — the layout -->
<svg xmlns="http://www.w3.org/2000/svg"
     xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1"
     viewBox="0 0 480 240">
  <use href="steel.svg" x="0" y="0"/>
  <use href="steel.svg" x="60" y="0" k:tags="powerup"/>  <!-- leaf tags: brick steel powerup -->
</svg>
```

### Group (GroupNode)

Bare `<g>` is the GroupNode carrier; groups nest recursively. A `<g>` may carry `fill`/`stroke`/
`stroke-width` as a presentational fallback for its children (resolved at parse time, baked into
per-shape styles, never re-emitted on the `<g>`).

```xml
<g fill="#ff0000">
  <circle cx="50" cy="50" r="10"/>
</g>
```
