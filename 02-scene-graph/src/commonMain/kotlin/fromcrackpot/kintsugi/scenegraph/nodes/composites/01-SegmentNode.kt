package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [Segment]: the segment's geometry carrying the concrete
 * [style] it renders with, plus optional [tags].
 *
 * Construction rebuilds the segment from [segment]'s endpoints — the given endpoint
 * instances are kept by reference, and rebuilding them into a segment reproduces the
 * geometry exactly for any finite segment. Everything a [Segment] offers is inherited, and
 * every operation typed against [Segment] accepts the leaf as-is — the shape-preserving
 * operations, [canonical] included, return through [copy] and come back as the leaf, style
 * and tags riding along; everything else returns plain shapes.
 *
 * @constructor Builds the leaf around [segment]'s endpoints.
 * @param segment The geometry to decorate; its endpoints are kept by reference.
 * @param style The concrete paint; the segment default when omitted.
 * @param tags Free-form tokens riding the leaf; empty by default.
 * @throws DegenerateDipoleException when [segment]'s endpoints rebuild no dipole — reachable
 *   only for a segment derived from an at-infinity dipole, whose endpoints degenerate.
 */
public open class SegmentNode(
    segment: Segment,
    override var style: ShapeStyle = ShapeStyle.SEGMENT,
    override var tags: Set<String> = emptySet(),
) : Segment(segment.start, segment.end), CompositeNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of [node]: the same geometry, and [node]'s own style and
     * tags unless either is overridden.
     *
     * Construction rebuilds the segment from [node]'s endpoints — the endpoint instances
     * are kept by reference, and rebuilding them reproduces the geometry exactly.
     * Overriding [style] or [tags] swaps that part alone — the other still comes from
     * [node].
     *
     * @param node The leaf to copy.
     * @param style The concrete paint; [node]'s own when omitted.
     * @param tags Free-form tokens riding the leaf; [node]'s own when omitted.
     */
    public constructor(
        node: SegmentNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as Segment, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [SegmentNode] around [segment], carrying this leaf's [style] and [tags].
     *
     * The leaf's rebuild hook: every shape-preserving operation returns through it, so the
     * moved leaf keeps its decoration.
     *
     * @param segment The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(segment: Segment): SegmentNode = SegmentNode(segment, style, tags)

    /*
    IDENTITY
    */

    /**
     * Rounded, style-inclusive identity: equal when the geometry matches at the `SCALE`
     * quantum and [style] and [tags] match exactly.
     *
     * Identity is exact-class: a `SegmentNode` and a plain [Segment] never compare equal,
     * in either direction.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a `SegmentNode` with equal rounded geometry, equal
     *   [style], and equal [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is SegmentNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry with the style and tags — full stored precision, no rounding. */
    override fun toString(): String = "SegmentNode(${super.toString()}, style=$style, tags=$tags)"
}
