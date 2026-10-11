package fromcrackpot.kintsugi.scenegraph.nodes

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.scenegraph.nodes.composites.CompositeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.PrimitiveNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A leaf of the scene graph: a CGA shape carrying the concrete [style] it renders with,
 * plus optional [tags].
 *
 * Every leaf class subclasses the shape it decorates — a `LineNode` *is* a `Line` — so a
 * leaf reads and operates as its shape: properties are inherited, and every operation typed
 * against the shape accepts the leaf as-is. The shape-preserving operations (`*`, the coerce
 * pair, `canonical`) return through the leaf's `copy` override, so they come back as the leaf
 * class with [style] and [tags] riding along; every other operation returns plain shapes —
 * the decoration drops by contract. [style] and [tags] are mutable: restyling or retagging a
 * leaf is an assignment, not a rebuild. Identity is exact-class and style-inclusive: a leaf
 * never equals the plain shape with the same geometry, and two leaves compare by rounded
 * geometry, [style], and [tags].
 *
 * Every leaf is either a [PrimitiveNode] — decorating a CGA element — or a [CompositeNode] —
 * decorating a shape built from primitives. `ShapeNode` itself is not sealed, so a `when`
 * over it is never exhaustive; the two families are, so exhaustive dispatch splits on them
 * and nests a `when` over each.
 */
public interface ShapeNode : Node {
    /** The concrete paint this leaf renders with; mutable — restyling is an assignment. */
    public var style: ShapeStyle

    /** Free-form tokens riding the leaf — renderer-inert; empty by default. Mutable — retagging is an assignment. */
    public var tags: Set<String>
}

/**
 * `node * Versor => ShapeNode`<br>
 * Returns this leaf with [versor]'s motion applied to its geometry — style and tags
 * untouched.
 *
 * The action for a leaf typed only as [ShapeNode]: it dispatches over the two sealed
 * families to the shape's own generic `*`, which returns through the leaf's `copy` override —
 * the result's runtime class is the receiver's, statically widened to [ShapeNode]. A leaf
 * with a concrete static type resolves to the generic shape action directly and keeps its
 * type.
 *
 * @receiver The leaf to move.
 * @param versor The motion to apply.
 * @return The moved leaf, same style and tags.
 * @throws DegeneratePolylineException when a polyline leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 */
public operator fun ShapeNode.times(versor: Versor): ShapeNode = when (this) {
    is PrimitiveNode -> when (this) {
        is FlatPointNode -> this * versor
        is LineNode -> this * versor
        is RoundPointNode -> this * versor
        is DipoleNode -> this * versor
        is CircleNode -> this * versor
    }
    is CompositeNode -> when (this) {
        is SegmentNode -> this * versor
        is PolylineNode -> this * versor
        is PolygonNode -> this * versor
    }
    else -> error("unknown ShapeNode family: ${this::class.simpleName}")
}
