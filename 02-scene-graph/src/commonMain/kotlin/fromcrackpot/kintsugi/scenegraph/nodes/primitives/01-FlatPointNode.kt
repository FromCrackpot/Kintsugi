package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [FlatPoint]: the point's geometry carrying the concrete
 * [style] it renders with, plus optional [tags].
 *
 * Construction copies [point]'s components, so the leaf carries the same geometry at full
 * `Double` precision. Everything a [FlatPoint] offers is inherited, and every operation
 * typed against [FlatPoint] accepts the leaf as-is — the shape-preserving operations return
 * through [copy] and come back as the leaf, style and tags riding along; everything else
 * returns plain shapes.
 *
 * @constructor Builds the leaf around [point]'s geometry.
 * @param point The geometry to decorate; its components are copied.
 * @param style The concrete paint; the point default when omitted.
 * @param tags Free-form tokens riding the leaf; empty by default.
 */
public open class FlatPointNode(
    point: FlatPoint,
    override var style: ShapeStyle = ShapeStyle.POINT,
    override var tags: Set<String> = emptySet(),
) : FlatPoint(point.px, point.py, point.pz), PrimitiveNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of [node]: the same geometry, and [node]'s own style and
     * tags unless either is overridden.
     *
     * Construction copies [node]'s components, so the copy carries the same geometry at
     * full `Double` precision. Overriding [style] or [tags] swaps that part alone — the
     * other still comes from [node].
     *
     * @param node The leaf to copy.
     * @param style The concrete paint; [node]'s own when omitted.
     * @param tags Free-form tokens riding the leaf; [node]'s own when omitted.
     */
    public constructor(
        node: FlatPointNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as FlatPoint, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [FlatPointNode] around [flatPoint], carrying this leaf's [style] and [tags].
     *
     * The leaf's rebuild hook: every shape-preserving operation returns through it, so the
     * moved leaf keeps its decoration.
     *
     * @param flatPoint The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(flatPoint: FlatPoint): FlatPointNode = FlatPointNode(flatPoint, style, tags)

    /*
    IDENTITY
    */

    /**
     * Rounded, style-inclusive identity: equal when the geometry matches at the `SCALE`
     * quantum and [style] and [tags] match exactly.
     *
     * Identity is exact-class: a `FlatPointNode` and a plain [FlatPoint] never compare
     * equal, in either direction.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a `FlatPointNode` with equal rounded geometry, equal
     *   [style], and equal [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is FlatPointNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry with the style and tags — full stored precision, no rounding. */
    override fun toString(): String = "FlatPointNode(${super.toString()}, style=$style, tags=$tags)"
}
