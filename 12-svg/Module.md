# Module svg

The SVG persistence layer: `parseSvg` and `serializeSvg` convert between SVG text and the scene
graph, in the `/v1` format. Every CGA shape rides a plain SVG element that any viewer renders, and
the `k:` sidecar namespace carries the CGA semantics SVG can't express. Files store values at full
`Double` precision; canonical equality is rebuilt on load through the shapes' rounded identity.

# Package fromcrackpot.kintsugi.svg

The two entry points with their fetch companion `scanSvgRefs`, the JVM `File` conveniences, and
the sealed `SvgFormatException` hierarchy naming every way a document or scene can fail the
format.

## The element vocabulary

Seven SVG elements carry the eight CGA shapes and the group tree. Element identity is resolved by
`k:type`, never by geometry.

| Element | Reading |
|---------|---------|
| `<rect>` | a rectangle `Polygon` (parse-only: a save writes `<polygon>`); a `FlatPoint` when `k:type="flat-point"` |
| `<circle>` | a `Circle`; a `RoundPoint` when `k:type="round-point"` |
| `<line>` | a `Segment`; an infinite `Line` when `k:type="line"` |
| `<polygon>` | a `Polygon` |
| `<polyline>` | a `Polyline` |
| `<g>` | a nested `GroupNode`; a `Dipole` when `k:type="dipole"` |
| `<use href="doc.svg">` | parse-only expansion of an external template document. A save writes the placed shapes |

## Loading: styles and templates

- **Styles arrive concrete.** Root-`<svg>` and `<g>` presentation attributes cascade nearest-wins
  into each shape's unset slots, and `k:tags` union down onto every leaf. No inheritance escapes
  into the scene.
- **Templates are fetched first.** `parseSvg` expands an external `<use href="doc.svg">` from its
  `templates` map, keyed by the `href` up to any `#`. The use site's attributes and tags flow into
  the template's unset slots. `scanSvgRefs` lists a document's references: scan the layout, fetch
  each document, re-scan those, then parse once with the full map.

## Saving

Degenerate CGA states persist faithfully:

- imaginary and inside-out circles and imaginary segments ride `k:flags`;
- an imaginary dipole needs no flag at all, since its round-point children rebuild it exactly;
- an infinite line writes display endpoints on the scene's world border.

Only a shape the format cannot place is skipped, with a warning: a point, line, segment or dipole
endpoint at infinity, and a zero-length segment or dipole, whose coincident endpoints would not
load back.
