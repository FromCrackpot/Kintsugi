package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.max
import kotlin.math.min

/**
 * Closed loop of straight edges through a cycle of vertices.
 *
 * The closed sibling of [Polyline]: N [vertices] make N [edges], the last edge closing back to
 * the first vertex implicitly. Starting vertex and traversal direction are data — the rounded
 * identity of [equals] distinguishes both. Counter-clockwise winding is the documented
 * convention but is not enforced; to read the actual winding, test [signedArea]'s sign.
 * Convexity is not required: self-intersecting (a pentagram) and self-touching (revisiting a
 * vertex mid-loop — a figure-eight, a slit) loops are legal; only consecutive coincident
 * vertices are rejected at construction.
 *
 * @constructor Validates the vertex list and throws on degenerate input.
 * @throws DegeneratePolygonException When fewer than 3 vertices are given, a vertex lies at
 *   infinity, or two consecutive vertices coincide at the `SCALE` quantum — including the
 *   last and first, whose implicit closing edge would collapse.
 */
public open class Polygon(
    /** The points the loop passes through — finite [FlatPoint]s in traversal order, closing implicitly. */
    public val vertices: List<FlatPoint>,
) : Composite {

    /*
    CTOR
     */

    init {
        if (vertices.size < 3) {
            throw DegeneratePolygonException("needs at least 3 vertices, got ${vertices.size}")
        }
        vertices.forEachIndexed { i, vertex ->
            if (vertex.isAtInfinity) {
                throw DegeneratePolygonException("vertex $i is at infinity: $vertex")
            }
            val next = vertices[(i + 1) % vertices.size]
            if (vertex == next) {
                throw DegeneratePolygonException("coincident consecutive vertices at $i: ${vertex.toStringRounded()}")
            }
        }
    }

    /*
    PROPS
     */

    /**
     * The edges, one [Segment] per consecutive vertex pair — N for N vertices, the last edge
     * wrapping back to the first vertex.
     *
     * Adjacent edges share their endpoint instances, wrapping at the loop:
     * `edges[i].end === edges[i + 1].start`.
     */
    public val edges: List<Segment> by cached {
        vertices.indices.map { i -> Segment(vertices[i], vertices[(i + 1) % vertices.size]) }
    }

    /**
     * The open [Polyline] over the same walk.
     *
     * An N + 1-vertex chain that starts and ends on the first vertex, making the implicit
     * closing edge explicit.
     */
    public val toPolyline: Polyline by cached { Polyline(vertices + vertices.first()) }

    /**
     * The signed area of the loop — raw; positive when the winding is counter-clockwise,
     * negative when clockwise.
     *
     * The shoelace sum over the vertex cycle; its sign is the actual winding, whatever the
     * construction order. For a self-intersecting loop the area is algebraic — regions cancel
     * by winding — so it can vanish on a non-empty figure (a symmetric figure-eight).
     */
    public val signedArea: Double by cached {
        var sum = 0.0
        vertices.forEachIndexed { i, a ->
            val b = vertices[(i + 1) % vertices.size]
            sum += a.px * b.py - b.px * a.py
        }
        sum / 2 + 0.0
    }

    /**
     * Whether this polygon is inside-out — wound clockwise, its solid region the closed
     * complement of the loop's bounded one.
     *
     * `true` when the raw [signedArea] is negative: the winding is the polygon's orientation,
     * and a clockwise loop encloses everything outside itself. Reversing the vertex order
     * flips it (`-polygon`). A loop whose raw area is exactly zero — a self-cancelling
     * figure — is not inside-out.
     */
    public val isInsideOut: Boolean get() = signedArea < 0.0

    /**
     * The area centroid — the balance point of the enclosed region — as a finite [FlatPoint].
     *
     * Shoelace-weighted; when [signedArea] rounds to zero (a self-cancelling loop) it falls
     * back to the vertex mean, so a well-defined point always comes back.
     */
    public val centroid: FlatPoint by cached {
        val a = signedArea
        if (round(a) == 0.0) {
            FlatPoint(vertices.sumOf { it.px } / vertices.size, vertices.sumOf { it.py } / vertices.size)
        } else {
            var cx = 0.0
            var cy = 0.0
            vertices.forEachIndexed { i, p ->
                val q = vertices[(i + 1) % vertices.size]
                val cross = p.px * q.py - q.px * p.py
                cx += (p.px + q.px) * cross
                cy += (p.py + q.py) * cross
            }
            FlatPoint(cx / (6 * a), cy / (6 * a))
        }
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [polygon] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves or reshapes a polygon computes its result as a plain
     * [Polygon] and returns it through this hook. This base implementation returns [polygon]
     * itself; a subclass overrides it to rebuild an instance of its own class around
     * [polygon], and every such operation then comes back as that class. The result's runtime
     * class must equal the receiver's — the operations check it and fail with a named error
     * when a subclass misses the override.
     *
     * @param polygon The replacement geometry.
     * @return [polygon] itself; an override returns its own class rebuilt around [polygon].
     */
    public open fun copy(polygon: Polygon): Polygon = polygon

    /*
    IDENTITY
    */

    /**
     * Rounded identity over the vertex cycle: equal when the vertex lists match pairwise at
     * the `SCALE` quantum.
     *
     * Starting vertex and traversal direction both count — the same loop entered at a
     * different vertex, or traversed in reverse, compares unequal. Identity is exact-class:
     * a subclass instance and a plain [Polygon] never compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its vertices all match
     *   after rounding.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Polygon
        return vertices == other.vertices
    }

    private val _hashCode by cached { vertices.hashCode() }

    /** Returns the hash of the rounded vertices, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the vertices — raw components, full stored precision. */
    override fun toString(): String = "Polygon(vertices=$vertices)"

    /** Returns the vertices rounded at the `SCALE` quantum — the debug-output convention of every shape. */
    public fun toStringRounded(): String = "Polygon(vertices=${vertices.map { it.toStringRounded() }})"

    /*
    FACTORY
     */

    public companion object {

        /**
         * Builds the axis-aligned rectangle spanning two opposite corners.
         *
         * Any opposite pair in any order — the coordinates are normalized to min/max. The
         * result is always the counter-clockwise quad bottom-left → bottom-right → top-right →
         * top-left, so equal rectangles compare equal with no canonicalization and the edge
         * indices are fixed: [edges]`[0]` bottom, `[1]` right, `[2]` top, `[3]` left. Integer
         * indexing is structural — a moved rectangle keeps its indices — while [RectSide]
         * indexing is visual, following the sides through any pose.
         *
         * @param a One corner.
         * @param b The opposite corner.
         * @return The four-vertex [Polygon] of the rectangle.
         * @throws DegeneratePolygonException When a corner lies at infinity, or the corners
         *   share a rounded x or y — zero extent, the quad collapses.
         */
        public fun Rectangle(a: FlatPoint, b: FlatPoint): Polygon {
            if (a.isAtInfinity || b.isAtInfinity) {
                throw DegeneratePolygonException("rectangle corner at infinity: ${if (a.isAtInfinity) a else b}")
            }
            if (round(a.px) == round(b.px) || round(a.py) == round(b.py)) {
                throw DegeneratePolygonException(
                    "rectangle corners share a rounded x or y — zero extent: ${a.toStringRounded()}, ${b.toStringRounded()}"
                )
            }
            val left = min(a.px, b.px)
            val right = max(a.px, b.px)
            val bottom = min(a.py, b.py)
            val top = max(a.py, b.py)
            return Polygon(
                listOf(
                    FlatPoint(left, bottom),
                    FlatPoint(right, bottom),
                    FlatPoint(right, top),
                    FlatPoint(left, top),
                )
            )
        }

        /**
         * Builds the triangle over three vertices in traversal order.
         *
         * @param a The first vertex.
         * @param b The second vertex.
         * @param c The third vertex.
         * @return The three-vertex [Polygon].
         * @throws DegeneratePolygonException When a vertex lies at infinity, or two of the
         *   three coincide at the `SCALE` quantum.
         */
        public fun Triangle(a: FlatPoint, b: FlatPoint, c: FlatPoint): Polygon = Polygon(listOf(a, b, c))
    }
}

/**
 * `-Polygon => Polygon`<br>
 * Returns this polygon turned inside out — the same loop walked in reverse.
 *
 * Reverses the vertex order, flipping the winding and the sign of [Polygon.signedArea]: a
 * classic loop comes back inside out and vice versa ([Polygon.isInsideOut] flips), so its solid
 * region is the complement of this polygon's. The vertex instances are kept — only the traversal
 * direction changes — and reversing twice restores the original vertex list exactly. Identity is traversal-sensitive, so a polygon and its
 * reversal compare unequal. The result is the receiver's own class: the reversed geometry
 * returns through [Polygon.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Polygon] to turn inside out.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed polygon, as the receiver's class.
 */
public operator fun <T : Polygon> T.unaryMinus(): T =
    narrowed(copy(Polygon(vertices.reversed())))

/**
 * The four visually named sides of a rectangle.
 *
 * Declaration order is [Polygon.Rectangle]'s counter-clockwise walk — bottom, right, top,
 * left — so on a freshly built rectangle each entry's `ordinal` is also its edge's index in
 * the rectangle's [Polygon.edges]. The names themselves are visual, not structural:
 * `edges[side]` answers by outward direction, so it keeps naming the visually right side
 * after mirrors or negation shuffle the indices; integer indexing stays the structural lane.
 */
public enum class RectSide {
    /** The side facing down — outward direction `(0, −1)`. */
    BOTTOM,

    /** The side facing right — outward direction `(1, 0)`. */
    RIGHT,

    /** The side facing up — outward direction `(0, 1)`. */
    TOP,

    /** The side facing left — outward direction `(−1, 0)`. */
    LEFT,
}

/**
 * `edges[RectSide] => Segment`<br>
 * Returns the edge that visually faces [side]'s way, whatever the loop's pose or orientation.
 *
 * A geometric lookup, not an index: the loop's winding is read off the edges themselves, each
 * edge's outward normal derived from its carrier, and the edge whose outward normal points
 * most nearly along [side]'s direction — down for [RectSide.BOTTOM], right for
 * [RectSide.RIGHT], up, left — is the answer, raw at full `Double` precision. On a freshly
 * built [Polygon.Rectangle] the answers coincide with the ordinals (`edges[BOTTOM]` is
 * `edges[0]`); after a motor, a mirror, or negation the lookup follows the visual side. The
 * answer is unambiguous while the rectangle is tilted less than 45° — at exactly 45° two
 * edges tie and the pick is arbitrary. Meaningful on a rectangle's [Polygon.edges]; any other
 * closed loop is scanned all the same, answering its most [side]-facing edge. `edges[RIGHT]`
 * spells the lookup.
 *
 * @receiver The edge loop to scan — a rectangle's [Polygon.edges] for the names to be truthful.
 * @param side The visual side whose edge to read.
 * @return The [Segment] facing [side]'s way.
 * @throws NoSuchElementException When the list is empty.
 */
public operator fun List<Segment>.get(side: RectSide): Segment {
    // Shoelace over the loop's edges: positive = counter-clockwise walk.
    var doubledArea = 0.0
    for (edge in this) doubledArea += edge.start.ax * edge.end.ay - edge.end.ax * edge.start.ay
    // Carrier normals point inward on a counter-clockwise loop — flip to outward.
    val outward = if (doubledArea < 0.0) 1.0 else -1.0
    val axisX = when (side) {
        RectSide.RIGHT -> 1.0
        RectSide.LEFT -> -1.0
        else -> 0.0
    }
    val axisY = when (side) {
        RectSide.TOP -> 1.0
        RectSide.BOTTOM -> -1.0
        else -> 0.0
    }
    return maxBy { edge ->
        val normal = edge.carrier.normal
        (normal.px * axisX + normal.py * axisY) * outward
    }
}
