package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [RoundPoint]: the point's geometry carrying the concrete [style] it
 * renders with, plus optional [tags].
 *
 * - Shape-preserving operations return the leaf, with its style and tags. Others return plain shapes.
 *
 * @constructor Builds the leaf around [point]'s geometry.
 * @param point The geometry to decorate.
 * @param style The concrete paint, the point default when omitted.
 * @param tags Free-form tokens riding the leaf, empty by default.
 */
public open class RoundPointNode(
    point: RoundPoint,
    override val style: ShapeStyle = ShapeStyle.POINT,
    override val tags: Set<String> = emptySet(),
) : RoundPoint(point.ax, point.ay, point.az, point.aw), PrimitiveNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of [node], with the same geometry. Style and tags are [node]'s own
     * unless overridden.
     *
     * @param node The leaf to copy.
     * @param style The concrete paint, [node]'s own when omitted.
     * @param tags Free-form tokens riding the leaf, [node]'s own when omitted.
     */
    public constructor(
        node: RoundPointNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as RoundPoint, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [RoundPointNode] around [roundPoint], carrying this leaf's [style] and [tags]. Every
     * shape-preserving operation returns through this rebuild hook.
     *
     * @param roundPoint The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(roundPoint: RoundPoint): RoundPointNode = RoundPointNode(roundPoint, style, tags)

    override fun copy(style: ShapeStyle, tags: Set<String>): RoundPointNode = RoundPointNode(this, style, tags)

    /*
    IDENTITY
    */

    /**
     * Compares the geometry rounded at the `SCALE` quantum, and [style] and [tags] exactly.
     *
     * - Classes must match exactly. A `RoundPointNode` never equals a plain [RoundPoint], in either direction.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a `RoundPointNode` with equal rounded geometry, [style], and [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is RoundPointNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry at full stored precision, with the style and tags. */
    override fun toString(): String = "RoundPointNode(${super.toString()}, style=$style, tags=$tags)"
}
