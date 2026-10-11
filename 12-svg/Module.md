# Module svg

The SVG persistence layer: `parseSvg` and `serializeSvg` between SVG text and the scene
graph, in the `/v1` format — every CGA shape rides a plain SVG element any viewer renders, and
the `k:` sidecar namespace carries the CGA semantics SVG can't express. Files store values at
full `Double` precision; canonical equality is rebuilt on load through the shapes' rounded
identity.

# Package fromcrackpot.kintsugi.svg

The two entry points with their fetch companion `scanSvgRefs`, the JVM `File` conveniences, and
the sealed `SvgFormatException` hierarchy naming every way a document or scene can fail the
format.

## The element vocabulary

Seven SVG elements carry the eight CGA shapes and the group tree. Element identity is resolved
by `k:type`, never by geometry.

| Element | Reading |
|---------|---------|
| `<rect>` | a rectangle `Polygon` (parse-only — a save writes `<polygon>`); a `FlatPoint` when `k:type="flat-point"` |
| `<circle>` | a `Circle`; a `RoundPoint` when `k:type="round-point"` |
| `<line>` | a `Segment`; an infinite `Line` when `k:type="line"` |
| `<polygon>` | a `Polygon` |
| `<polyline>` | a `Polyline` |
| `<g>` | a nested `GroupNode`; a `Dipole` when `k:type="dipole"` |
| `<use href="doc.svg">` | parse-only expansion of an external template document — a save writes the placed shapes |

Degenerate CGA states persist faithfully: imaginary and inside-out circles ride `k:flags`, an
imaginary dipole needs no flag at all (its round-point children rebuild it exactly), and an
infinite line writes display endpoints on the scene's world border. Only a shape the format
cannot place — a line or dipole endpoint at infinity — is skipped with a warning on save.
