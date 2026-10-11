# Kintsugi SVG `/v1` — normative format spec

The authoritative rules for the `/v1` format implemented by `parseSvg`/`serializeSvg`.
`CGA-SVG-MAP.md` (this dir) is the at-a-glance quick-reference — namespace, mapping table, one
example per shape. Seven SVG elements carry eight CGA shapes plus `GroupNode` (six shape/group
carriers plus the parse-only `<use>` reference).

## Foundations

- **Spec target: SVG 1.1 Full, plus two `/v1` extensions.** Browsers implement 1.1 as the stable
  baseline; SVG 2 is partial and per-vendor. Don't reach for further SVG 2-only constructs without
  spec'ing them here. The extensions: the `k:` sidecar namespace
  (`xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1"`, valid 1.1), and **external
  whole-document `<use href="doc.svg">`** (§Elements) — the one construct where `/v1` runs ahead
  of renderers: fragment-free external `<use>` is where the SVG spec is heading but today renders
  only in modern Chromium (Chrome/Edge 137+), only same-origin (serve over `http://localhost`;
  `file://` double-click shows nothing), and not in Firefox, Safari, or IDE previews. The
  *expanded* form a save produces is plain 1.1 and renders everywhere. Still no
  `<symbol>`/`<defs>`/internal `#id` references — those (plus fragment addressing for
  multi-template library files) are the documented re-entry path if broad-viewer preview of
  layout files ever matters; note that adding fragments would reopen the `<use>` anchoring
  decision (origin-anchored per browser semantics either way, but internal refs *must* match
  what browsers draw).
- **Renderable carrier + `k:*` sidecar.** Each primitive rides a plain SVG element that renders in
  any viewer; `k:*` attributes carry CGA semantics SVG can't express. Element identity is resolved
  by `k:type`, **never by geometry**. Read `k:*` with the explicit namespace
  (`XmlReader.kAttr(name)`), NOT `getAttributeValue(null, name)` — xmlutil treats a `null`
  namespace as "any namespace," which would also match a plain `type`. Plain attribute reads stay
  `getAttributeValue(null, name)`.
- **viewBox-based Y-flip.** Loader applies `(x, y) → (x − vbMinX − vbW/2, vbMinY + vbH/2 − y)` so
  loaded scenes display right-side up; inverse in the serializer. viewBox is preserved on the root
  `GroupNode.bounds` for exact round-trip. Only the root `bounds` round-trips (via `<svg viewBox>`);
  a non-null nested bounds skip+warns on serialize and re-parses as `null`. Serialize throws
  `MissingViewBoxException` when root `bounds` is null.
- **Persist ≠ draw.** Degenerate CGA states `:compose` can't draw (imaginary radius, inside-out
  orientation) are persisted faithfully here — imaginary objects are first-class CGA data; the
  data-loss decision is never the persistence layer's to make.
- **Single ordered child list.** Each parsed shape becomes a typed shape-node leaf with a fully concrete
  style; nested bare `<g>` ↔ nested `GroupNode`. Source order across mixed shapes and groups is
  preserved both ways (z-order is stable); the serializer walks the tree (never `flattenShapes`).
- **No background concept.** A scene backdrop is an ordinary full-bleed rectangle `Polygon` first
  child, encoded/decoded like any other shape. Don't reintroduce geometry-keyed detection
  heuristics (a promoted "background rect" made a genuine full-bleed-rect sprite unloadable).

## Elements

`<rect>`, `<circle>`, `<line>`, `<polygon>`, `<polyline>`, `<g>`, `<use>` are the supported
elements; any other raises `UnsupportedSvgElementException`. On every element, an *unknown*
`k:type` warns and falls back to the untagged default reading.

- **`<rect>`** — `FlatPoint` when `k:type="flat-point"`; otherwise a **rectangle `Polygon`**
  (parse-only reading: decodes through `Polygon.Rectangle` into the CCW quad bl → br → tr → tl —
  `:cga`'s factory-order invariant — and **re-serializes as `<polygon>`**; lossy at the element
  level, lossless at the CGA level, stable from the second save). `k:flags="inside-out"` reverses
  the factory walk, decoding the rect as an inside-out rectangle `Polygon` (the wall reading).
  The rectangle reading requires
  all of `x`/`y`/`width`/`height` (a designer rect without extents is a mistake), rejects non-zero
  `rx`/`ry` (no arcs in `/v1`) and **negative extents** (an SVG error must stay an error — the
  factory's min/max normalization would silently re-anchor it), and wraps
  `DegeneratePolygonException` as `MalformedRectException`.
- **`<circle>`** — `Circle` by default (designer circles "just work"); `RoundPoint` when
  `k:type="round-point"`. Discriminated strictly by `k:type`, never by `r` — never promote/demote
  on radius.
- **`<line>`** — `Segment` by default (`Segment(FlatPoint(x1,y1), FlatPoint(x2,y2))`, endpoints
  stored as given); infinite `Line` when `k:type="line"` (`Line = FlatPoint(x1,y1) join
  FlatPoint(x2,y2)`; the serializer lands endpoints on the world border for display, but
  recognition depends only on the tag). A tagged line with missing attributes throws
  `MalformedLineException`; a bare line, `MalformedSegmentException`. Coincident endpoints: on a
  tagged line, allowed at parse (degenerate `Line`; the serializer's `isAtInfinity` skip+warn
  handles it downstream); on a bare line, `MalformedSegmentException`. `fill` is ignored per SVG
  `<line>` semantics.
- **`<polygon>`** — `Polygon` (untagged only). Requires `points` (whitespace/comma pairs; an odd
  coordinate count throws `MalformedPolygonException` rather than dropping the trailing number as
  browsers do); per-vertex Y-flip, then the walk is **canonicalized against `k:flags`** — see
  "Polygon orientation" under `k:*` semantics; wraps `DegeneratePolygonException`.
- **`<polyline>`** — `Polyline` (untagged only), mirroring `<polygon>` with the open chain's looser
  floor (≥ 2 pairs; first == last preserved as data). Honors `stroke-linecap` (open stroke); wraps
  `DegeneratePolylineException` as `MalformedPolylineException`.
- **`<g>`** — `GroupNode` by default; **`Dipole`** when `k:type="dipole"`. The dipole carrier holds
  two `<circle k:type="round-point">` children (child order = orientation: first child is the left
  operand of `start ∧ end`) plus an inline dashed `<line>` visual chrome the parser **ignores
  entirely**. The Containers invariant `start ∧ end = D` makes the encoding canonical **including
  imaginary dipoles** — no flags needed. Coincident endpoints raise `MalformedDipoleException`;
  at-infinity endpoints skip+warn at serialize (can't encode as `<circle>`). Designer `<g>`
  attributes (`id`, `inkscape:*`) are silently dropped and never emitted.
- **`<use href="doc.svg" x=".." y=".."/>`** — **parse-only template expansion** (the `<rect>`
  precedent: lossy at the element level, lossless at the CGA level — a save writes the expanded
  placed shapes, never a `<use>`). `href` is required and names a whole external document; a
  `#fragment` suffix is tolerated and ignored (the path before `#` keys resolution); a
  bare-fragment `#id` (internal reference) is `MalformedUseException`. Resolution is
  caller-supplied: `parseSvg(content, templates: Map<String, String>)` maps href → the template's
  **raw SVG text** (text, not parsed trees — each use site re-parses its template inside the
  site's inherited chain, because explicit-vs-unset is unknowable after styles bake); a missing
  entry is `MissingTemplateException`; `scanSvgRefs(content)` is the fetch pass. **Origin
  anchoring** per browser semantics: the template's viewport top-left lands at `(x, y)`
  (default 0). **Template contract**: the referenced root `<svg>` must declare explicit
  `width`/`height` equal to its viewBox extents (`MalformedTemplateException` — a sizeless
  nested svg renders at 100% in browsers; the load boundary turns the silent blowup into a
  diagnosis). **No scaling ever**: `width`/`height` on `<use>` is `MalformedUseException` (one
  CGA unit = one SVG user unit in every document — a bigger brick is a different template).
  `<use>` must be empty. Use-site presentation attrs and `k:tags` seed the template's
  inheritance chain (SVG's cascade into the use shadow tree): unset template slots inherit
  per-instance fill/stroke/tags, explicit template attrs win, tags union onto every expanded
  leaf. Templates may `<use>` other templates (resolved from the same map); a closed loop is
  `SvgTemplateCycleException` naming the chain. The expansion splices as one bare `GroupNode`
  (bounds `null`) wrapping the template's children.

## `k:*` semantics

- **Round-point `r` is the render marker size (`size/2`), not radius²; `k:aw` carries the algebra**
  — the raw e4 weight, fed straight into the 4-arg `RoundPoint(ax, ay, 1, aw)` ctor (no quadratic
  recombination; store-exact). The serializer **always emits `k:aw`**; the parser tolerates its
  absence on hand-authored input (falls back to the 2-arg null-cone lift, so the file gains a
  `k:aw` on first save). `k:aw` is a CGA-space value — hand-authors need CGA coords. Imaginary
  round points need no flag (a small enough `k:aw` is imaginary on its own).
- **Render size rides in the element geometry**: flat-point `<rect>` is a square of side
  `ShapeStyle.size` centered on the position; round-point `r = size/2`. The parser sources `size`
  back from the geometry, never from `defaultFor`. A non-square flat-point rect is
  `MalformedRectException`.
- **Imaginary/inside-out circles persist via `k:flags`** (token list: `imaginary`, `inside-out`)
  on a plain `<circle cx cy r>` with `cx/cy` = geometric center, `r = √|radius²|`. Parse
  reconstructs exactly via the **raw 4-arg `Circle` ctor** (build the classic circle with signed
  radius², negate all components for the inside-out twin — the friendly ctor only yields real
  classic circles). Flag parsing is tolerant (trim/lowercase/split; unknown tokens warn).
  `imaginary` derives from the *rounded* `isImaginary` — a sub-quantum imaginary circle saves
  flagless and re-parses identity-equivalent; don't "fix" by deriving from the raw sign.
  (`:compose` still skips these at the draw site — persist ≠ draw.)
- **Polygon orientation: `k:flags="inside-out"` is intent, model winding is truth.** On parse
  the model-space walk (post Y-flip) is reversed when its winding contradicts the flag: flagged
  loops come out clockwise (`Polygon.isInsideOut`, the wall reading — solid region is the closed
  complement), unflagged loops counter-clockwise (classic). A zero-area loop has no orientation
  to flip; the flag warns and is ignored. On serialize the flag is emitted exactly when
  `isInsideOut`, and vertices are written in stored order — flag and winding agree by invariant,
  so a re-parse reverses nothing and round-trips are idempotent. Consequence: raw `points` order
  is canonicalized once at parse (a miswound hand-authored walk is reversed); from the first
  save onward the document is stable. Same flag grammar and tolerance as the circle's.
- **`k:tags` — open-vocabulary game-semantic tokens** (`ShapeNode.tags`), legal on every shape
  element, on `<g>`, and on `<use>`. Same token grammar as `k:flags` (trim, split on whitespace,
  drop empties) but an open vocabulary: **no known-token list, no unknown-token warn, no
  lowercasing** — tags are user vocabulary, preserved verbatim. Renderer-inert by definition
  (`:compose` never reads them); games dispatch on them (a brick's type at collision). Group and
  use-site tags **union** into descendant leaves at parse (the style-cascade bake idiom — nothing
  is ever stored on `GroupNode`); on the dipole they ride the carrier `<g>` and `k:tags` on its
  round-point children is ignored (mirrored at serialize).

## Styles

- **Color grammar** (`svg.parse.parseColor`): `none`, `#rgb`, `#rrggbb`, `#rgba`, `#rrggbbaa`,
  `rgb()`, `rgba()`. Anything else (named colors, `currentColor`, `inherit`, `url()`, `hsl()`,
  percentages, slash syntax) throws `UnsupportedPaintException` with the raw value and location.
  `style="…"` CSS form is out of scope.
- **Zero-alpha collapses to `none`** both ways — one representation of "no paint"; SVG
  pointer-events fidelity is deliberately traded away (out of scope for Kintsugi).
- **SVG defaults are materialized at parse time** (`SvgDefaults`: `fill=#000000`, `stroke=none`,
  `stroke-width=1`) — every parsed shape carries a fully concrete `ShapeStyle`; renderers never
  re-derive SVG semantics.
- **Root `<svg>` and `<g>` presentation attributes resolve at the parse boundary** into per-shape
  concrete styles: own attr → ancestor-`<g>` chain (nearest-first) → root-`<svg>` → `SvgDefaults`
  (`parsing/InheritedStyle.kt`), scoped to the attrs each element consumes. Tagged `<g k:type=…>`
  carriers overlay their own `fill` on the inherited chain like any shape parser. No model-level
  inheritance ever escapes into `:scene-graph`. The root `<svg>` tag itself emits `viewBox` + namespaces
  only — never `fill`/`stroke`/`stroke-width`, even when the input carried them.
- **`stroke-linecap` applies to `<line>`/segment/`<polyline>` only — never `<circle>`/`<polygon>`**
  (closed paths have no caps; the same "general field, skipped by elements that can't use it"
  precedent as `fill` on `<line>`). Parse is tolerant: unknown keyword warns and falls back to the
  cascade (cosmetic, unlike `fill`'s throw). It cascades like `stroke-width`.

## Serialization

- **Two attribute tiers — core vs optional-feature** (the axis is core-vs-optional, NOT
  matches-the-SVG-default):
  - **Core: always emitted**, even when equal to the SVG default, so `:svg` and `:compose` never
    depend on a viewer supplying one: `fill`/`stroke`/`stroke-width` per element's set, `k:type`
    where tagged, `k:aw` always on round-point (tolerated-absent on parse yet always written —
    proof the axis isn't has-a-default).
  - **Optional-feature: emitted only when active**: `stroke-linecap` only when non-`butt`,
    `k:flags` only when set, `k:tags` only when non-empty (space-joined, insertion order,
    emitted last on every element; on the dipole it sits on the carrier `<g>` after `k:type`).
- **Canonical output, full-precision numerics.** Numbers are shortest round-trippable
  `Double.toString` (never SCALE-quantized); signed zero collapses to `"0"`; integers omit the
  decimal. Colors are canonical lowercase hex (`#rrggbb` / `#rrggbbaa`; zero-alpha → `"none"`) —
  round-trip is canonical-form, not byte-verbatim (`#F00` → `#ff0000`), and
  `parseSvg(serializeSvg(scene)) == scene` holds. Fixed attribute order (`k:type` first;
  xmlutil's `KtXmlWriter` preserves call order): flat-point `k:type x y width height fill`;
  round-point `k:type cx cy r k:aw fill`; circle `cx cy r fill stroke stroke-width [k:flags]`;
  line `k:type x1 y1 x2 y2 stroke stroke-width [stroke-linecap]` (segment: same minus `k:type`);
  rectangle-polygon `points fill stroke stroke-width [k:flags]`; polyline adds
  `[stroke-linecap]`. Writer config: no trailing space before `/>`, `XmlVersion.XML10`,
  `XmlDeclMode.Minimal`.
- **Cosmetic FP drift across save cycles is normal** under exact storage (`255.79` →
  `255.78999999999996` after a viewBox flip) — re-parsing is idempotent at the Double level.
  **Canonical output is per-platform**: `Double.toString` diverges between JVM and wasmJs (`1.0E7`
  vs `10000000`) — both valid; never golden-file or content-hash serializer output cross-platform.
- **Infinite shapes serialize via the world-border idiom** (`scenegraph`'s `worldBorder`; see
  `serializing/primitives/02-LineWriter.kt`) — build it once per serialize call, pass it down.
