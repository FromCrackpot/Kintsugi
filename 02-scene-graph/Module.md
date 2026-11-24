# Module scene-graph

The renderer-neutral scene graph: a tree of `GroupNode` sub-trees and shape-node leaves. Each
leaf *is* its CGA shape: it subclasses the shape and carries the concrete `ShapeStyle` it renders
with. The module also holds the `SceneBounds` viewport rectangle and the shared paint vocabulary
(`ShapeColor`, `ShapeStrokeCap`, `ShapeDash`, `DipoleChrome`, `ImaginaryChrome`) that every
renderer reads identically.

A leaf is an immutable value, decoration included. `node * versor` returns a new node through the
leaf's geometry `copy` override, and `restyled`/`retagged` return one through its decoration
`copy` override. Equality is structural and exact-class throughout:

- leaves compare by rounded geometry, style and tags, and a leaf never equals the plain shape with
  the same geometry;
- `GroupNode` compares across children and bounds, and a subclass never equals a plain group.

# Package fromcrackpot.kintsugi.scenegraph

Viewport geometry: `SceneBounds`, the axis-aligned rectangle a scene occupies (the root group's
bounds is the viewport), its bounding-box factory over a `Polygon`, and `worldBorder`, the
origin-centered circle renderers clip infinite lines against.

# Package fromcrackpot.kintsugi.scenegraph.nodes

The tree: the sealed `Node`, with its two kinds.

- `ShapeNode`: a styled, tagged leaf that subclasses its CGA shape.
- `GroupNode`: an ordered sub-tree with optional bounds.

A leaf reads and operates as its shape, and geometric operations accept it as-is. The
shape-preserving ones (`*`, the coerce pair, `canonical`) return through the leaf's `copy`
override, so the leaf comes back as its own class, with its style and tags. Everything else
returns plain shapes: the decoration drops by contract.

This package adds two transforms. The `ShapeNode * Versor` dispatch serves leaves typed only as
`ShapeNode`. `GroupNode * Versor` moves whole sub-trees. It is generic over the receiver and
returns through the group's own `copy` hook, so a `GroupNode` subclass comes back as its own class
too.

`ShapeNode` itself is not sealed. Every leaf belongs to one of the two sealed families one package
down: `PrimitiveNode`, the blade leaves, and `CompositeNode`, the composite leaves. So exhaustive
dispatch splits on the families and nests a `when` over each.

## The node-preserving transforms

| Pairing | Result |
|---------|--------|
| `node * Versor` | the leaf with its geometry moved. Each leaf class comes back as itself, style and tags untouched; the `ShapeNode`-typed dispatch widens the static type only |
| `GroupNode * Versor` | the group with every leaf in the sub-tree moved. Structure, order, styles and bounds are preserved, and a group subclass comes back as its own class through its `copy` hook |

# Package fromcrackpot.kintsugi.scenegraph.nodes.primitives

The blade leaves, one class per CGA element: `FlatPointNode`, `LineNode`, `RoundPointNode`,
`DipoleNode` and `CircleNode`. They sit under the sealed `PrimitiveNode` family marker. A `when`
over it is exhaustive without an `else`, so a new blade leaf is a compile error at every dispatch
site.

# Package fromcrackpot.kintsugi.scenegraph.nodes.composites

The composite leaves, `SegmentNode`, `PolylineNode` and `PolygonNode`, under the sealed
`CompositeNode` family marker. A `when` over it is exhaustive without an `else`, so a new
composite leaf is a compile error at every dispatch site. The CGA coerce pair applies to
`PolygonNode` directly: being generic over the receiver, it returns the leaf through its `copy`
override, style and tags untouched.

# Package fromcrackpot.kintsugi.scenegraph.style

The paint vocabulary:

- `ShapeStyle`, the concrete style every shape renders with, with its per-shape default constants
  and `defaultFor`. It has no cascade: "no paint" is `ShapeColor.NONE`, and "no thickness" is
  zero. The dash is its one nullable field, because an unset dash takes a shape-dependent default.
- `ShapeColor`, the packed-ARGB color carrier.
- `ShapeStrokeCap`, the open-stroke line cap.
- `ShapeDash`, the stroke dash pattern, in model units. `ShapeDash.SOLID` is an explicit solid.
- `DipoleChrome`, the shared convention for a dipole's connector line.
- `ImaginaryChrome`, the shared convention for imaginary shapes: dashed strokes and ring markers.

## Dash resolution

`ShapeStyle.dashFor` resolves the dash every stroke of a shape draws with. Both renderers call it.

| Style `dash` | Imaginary shape | Real dipole | Any other shape |
|--------------|-----------------|-------------|-----------------|
| set | the set pattern | the set pattern | the set pattern |
| `ShapeDash.SOLID` | solid | solid | solid |
| unset | `ImaginaryChrome.DASH` | `DipoleChrome.DASH` | solid |
