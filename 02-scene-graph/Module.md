# Module scene-graph

The renderer-neutral scene graph: a tree of `GroupNode` sub-trees and shape-node
leaves — each leaf *is* its CGA shape, subclassing it and carrying the concrete `ShapeStyle`
it renders with — plus the `SceneBounds` viewport rectangle and the shared paint vocabulary
(`ShapeColor`, `ShapeStrokeCap`, `DipoleChrome`) every renderer reads identically.

A leaf's geometry is an immutable value and moves by rebuilding — `node * versor` returns a
new node through the leaf's `copy` override — while its style and tags are mutable
assignments. Equality is structural and exact-class throughout: leaves compare by rounded
geometry, style, and tags — a leaf never equals the plain shape with the same geometry — and
`GroupNode` compares across children and bounds, a subclass never equal to a plain group.

# Package fromcrackpot.kintsugi.scenegraph

Viewport geometry: `SceneBounds`, the axis-aligned rectangle a scene occupies (the root
group's bounds is the viewport), its bounding-box factory over a `Polygon`, and
`worldBorder`, the origin-centered circle renderers clip infinite lines against.

# Package fromcrackpot.kintsugi.scenegraph.nodes

The tree: the sealed `Node` with its two kinds — `ShapeNode`, a styled, tagged leaf
that subclasses its CGA shape, and `GroupNode`, an ordered sub-tree with optional bounds.
A leaf reads and operates as its shape: geometric operations accept it as-is, and the
shape-preserving ones (`*`, the coerce pair, `canonical`) return through the leaf's `copy`
override — the leaf comes back as its own class, style and tags riding along; everything
else returns plain shapes — the decoration drops by contract. This package adds the
`ShapeNode * Versor` dispatch for leaves typed only as `ShapeNode`, and `GroupNode * Versor`
over whole sub-trees — generic over the receiver and returning through the group's own
`copy` hook, so a `GroupNode` subclass comes back as its own class too.

`ShapeNode` itself is not sealed; every leaf belongs to one of the two sealed families one
package down — `PrimitiveNode`, the blade leaves, and `CompositeNode`, the composite
leaves — so exhaustive dispatch splits on the families and nests a `when` over each.

## The node-preserving transforms

| Pairing | Result |
|---------|--------|
| `node * Versor` | the leaf with its geometry moved — each leaf class comes back as itself, style and tags untouched; the `ShapeNode`-typed dispatch widens the static type only |
| `GroupNode * Versor` | the group with every leaf in the sub-tree moved — structure, order, styles, and bounds preserved; a group subclass comes back as its own class through its `copy` hook |

# Package fromcrackpot.kintsugi.scenegraph.nodes.primitives

The blade leaves, one class per CGA element — `FlatPointNode`, `LineNode`, `RoundPointNode`,
`DipoleNode`, `CircleNode` — under the sealed `PrimitiveNode` family marker: a `when` over
it is exhaustive without an `else`, so a new blade leaf is a compile error at every
dispatch site.

# Package fromcrackpot.kintsugi.scenegraph.nodes.composites

The composite leaves — `SegmentNode`, `PolylineNode`, `PolygonNode` — under the sealed
`CompositeNode` family marker: a `when` over it is exhaustive without an `else`, so a new
composite leaf is a compile error at every dispatch site. The CGA coerce pair applies to
`PolygonNode` directly — generic over the receiver, it returns the leaf through its `copy`
override, style and tags untouched.

# Package fromcrackpot.kintsugi.scenegraph.style

The paint vocabulary: `ShapeStyle`, the concrete style every shape renders with — no nullable
fields, no cascade; "no paint" is `ShapeColor.NONE`, "no thickness" zero — with its per-shape
default constants and `defaultFor`; `ShapeColor`, the packed-ARGB color carrier;
`ShapeStrokeCap`, the open-stroke line cap; and `DipoleChrome`, the shared convention for a
dipole's connector line.
