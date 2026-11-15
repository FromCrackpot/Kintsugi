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
 * A leaf of the scene graph: a CGA shape carrying the concrete [style] it renders with, plus
 * optional [tags].
 *
 * - Each leaf class subclasses its shape, so a `LineNode` *is* a `Line` and every shape operation accepts it.
 * - Shape-preserving operations return the leaf, with its style and tags. Others return plain shapes.
 * - A leaf never equals the plain shape. Leaves compare by rounded geometry, [style] and [tags].
 */
public interface ShapeNode : Node {
    /** The concrete paint this leaf renders with. Another paint makes another leaf (see [restyled]). */
    public val style: ShapeStyle

    /** Free-form tokens riding the leaf, ignored by renderers. Empty by default (see [retagged]). */
    public val tags: Set<String>

    /**
     * Returns a leaf of this one's class with the same geometry and the given decoration. Restyling
     * and retagging return through this rebuild hook.
     *
     * @param style The concrete paint, this leaf's own when omitted.
     * @param tags Free-form tokens riding the leaf, this leaf's own when omitted.
     * @return The rebuilt leaf.
     */
    public fun copy(style: ShapeStyle = this.style, tags: Set<String> = this.tags): ShapeNode
}

/**
 * Returns this leaf painted with [style] instead. Geometry, tags and class stay the same.
 *
 * @receiver The leaf to restyle, left untouched.
 * @param N The receiver's type, preserved in the result.
 * @param style The replacement paint.
 * @return The restyled leaf.
 * @see retagged
 */
public fun <N : ShapeNode> N.restyled(style: ShapeStyle): N = narrowed(copy(style = style))

/**
 * Returns this leaf carrying [tags] instead. Geometry, style and class stay the same.
 *
 * @receiver The leaf to retag, left untouched.
 * @param N The receiver's type, preserved in the result.
 * @param tags The replacement tokens.
 * @return The retagged leaf.
 * @see restyled
 */
public fun <N : ShapeNode> N.retagged(tags: Set<String>): N = narrowed(copy(tags = tags))

/** Turns a wrong-class decoration `copy` result into a named error. This is the decoration verbs' one cast site. */
internal fun <N : ShapeNode> N.narrowed(copied: ShapeNode): N {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName}, not its own class"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as N
}

/**
 * `node * Versor => ShapeNode`<br>
 * Returns this leaf with [versor]'s motion applied to its geometry, for a leaf typed only as
 * [ShapeNode]. Style and tags stay untouched.
 *
 * @receiver The leaf to move.
 * @param versor The motion to apply.
 * @return The moved leaf, of the receiver's runtime class.
 * @throws DegeneratePolylineException when a polyline leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
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
