package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [Polygon]: the loop's geometry carrying the concrete
 * [style] it renders with, plus optional [tags].
 *
 * Construction reuses [polygon]'s vertex list by reference, so the leaf carries the same
 * geometry at full `Double` precision. Everything a [Polygon] offers is inherited, and
 * every operation typed against [Polygon] accepts the leaf as-is — the shape-preserving
 * operations, the coerce pair included, return through [copy] and come back as the leaf,
 * style and tags riding along; everything else returns plain shapes.
 *
 * @constructor Builds the leaf around [polygon]'s vertices.
 * @param polygon The geometry to decorate; its vertex list is kept by reference.
 * @param style The concrete paint; the polygon default when omitted.
 * @param tags Free-form tokens riding the leaf; empty by default.
 */
public open class PolygonNode(
    polygon: Polygon,
    override var style: ShapeStyle = ShapeStyle.POLYGON,
    override var tags: Set<String> = emptySet(),
) : Polygon(polygon.vertices), CompositeNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of [node]: the same geometry, and [node]'s own style and
     * tags unless either is overridden.
     *
     * Construction reuses [node]'s vertex list by reference, so the copy carries the same
     * geometry at full `Double` precision. Overriding [style] or [tags] swaps that part
     * alone — the other still comes from [node].
     *
     * @param node The leaf to copy.
     * @param style The concrete paint; [node]'s own when omitted.
     * @param tags Free-form tokens riding the leaf; [node]'s own when omitted.
     */
    public constructor(
        node: PolygonNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as Polygon, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [PolygonNode] around [polygon], carrying this leaf's [style] and [tags].
     *
     * The leaf's rebuild hook: every shape-preserving operation returns through it, so the
     * moved leaf keeps its decoration.
     *
     * @param polygon The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(polygon: Polygon): PolygonNode = PolygonNode(polygon, style, tags)

    /*
    IDENTITY
    */

    /**
     * Rounded, style-inclusive identity: equal when the geometry matches at the `SCALE`
     * quantum and [style] and [tags] match exactly.
     *
     * Identity is exact-class: a `PolygonNode` and a plain [Polygon] never compare equal,
     * in either direction.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a `PolygonNode` with equal rounded geometry, equal
     *   [style], and equal [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is PolygonNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry with the style and tags — full stored precision, no rounding. */
    override fun toString(): String = "PolygonNode(${super.toString()}, style=$style, tags=$tags)"
}
