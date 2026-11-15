package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [Circle]: the circle's geometry carrying the concrete [style] it
 * renders with, plus optional [tags].
 *
 * - Shape-preserving operations return the leaf, with its style and tags. Others return plain shapes.
 *
 * @constructor Builds the leaf around [circle]'s geometry.
 * @param circle The geometry to decorate.
 * @param style The concrete paint, the circle default when omitted.
 * @param tags Free-form tokens riding the leaf, empty by default.
 */
public open class CircleNode(
    circle: Circle,
    override val style: ShapeStyle = ShapeStyle.CIRCLE,
    override val tags: Set<String> = emptySet(),
) : Circle(circle.cx, circle.cy, circle.cz, circle.cw), PrimitiveNode {

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
        node: CircleNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as Circle, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [CircleNode] around [circle], carrying this leaf's [style] and [tags]. Every
     * shape-preserving operation returns through this rebuild hook.
     *
     * @param circle The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(circle: Circle): CircleNode = CircleNode(circle, style, tags)

    override fun copy(style: ShapeStyle, tags: Set<String>): CircleNode = CircleNode(this, style, tags)

    /*
    IDENTITY
    */

    /**
     * Compares the geometry rounded at the `SCALE` quantum, and [style] and [tags] exactly.
     *
     * - Classes must match exactly. A `CircleNode` never equals a plain [Circle], in either direction.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a `CircleNode` with equal rounded geometry, [style], and [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is CircleNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry at full stored precision, with the style and tags. */
    override fun toString(): String = "CircleNode(${super.toString()}, style=$style, tags=$tags)"
}
