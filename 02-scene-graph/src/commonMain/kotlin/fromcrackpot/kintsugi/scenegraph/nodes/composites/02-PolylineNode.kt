package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [Polyline]: the chain's geometry carrying the concrete [style] it
 * renders with, plus optional [tags].
 *
 * - Shape-preserving operations return the leaf, with its style and tags. Others return plain shapes.
 * - `polyline`'s vertex list is kept by reference.
 *
 * @constructor Builds the leaf around `polyline`'s geometry.
 * @param polyline The geometry to decorate.
 * @param style The concrete paint, the polyline default when omitted.
 * @param tags Free-form tokens riding the leaf, empty by default.
 */
public open class PolylineNode(
    polyline: Polyline,
    override val style: ShapeStyle = ShapeStyle.POLYLINE,
    override val tags: Set<String> = emptySet(),
) : Polyline(polyline.vertices), CompositeNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of `node`, with the same geometry. Style and tags are `node`'s own
     * unless overridden.
     *
     * @param node The leaf to copy.
     * @param style The concrete paint, `node`'s own when omitted.
     * @param tags Free-form tokens riding the leaf, `node`'s own when omitted.
     */
    public constructor(
        node: PolylineNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as Polyline, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [PolylineNode] around [polyline], carrying this leaf's [style] and [tags]. Every
     * shape-preserving operation returns through this rebuild hook.
     *
     * @param polyline The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(polyline: Polyline): PolylineNode = PolylineNode(polyline, style, tags)

    override fun copy(style: ShapeStyle, tags: Set<String>): PolylineNode = PolylineNode(this, style, tags)

    /*
    IDENTITY
    */

    /**
     * Compares the geometry rounded at the `SCALE` quantum, and [style] and [tags] exactly.
     *
     * - Classes must match exactly. A `PolylineNode` never equals a plain [Polyline], in either direction.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a `PolylineNode` with equal rounded geometry, [style], and [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is PolylineNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry at full stored precision, with the style and tags. */
    override fun toString(): String = "PolylineNode(${super.toString()}, style=$style, tags=$tags)"
}
