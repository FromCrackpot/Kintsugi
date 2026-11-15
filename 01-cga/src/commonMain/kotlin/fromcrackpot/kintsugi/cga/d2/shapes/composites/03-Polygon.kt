package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.InsideOutPolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonConvexPolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.*

/**
 * A closed loop of straight edges through a cycle of vertices. N vertices make N [edges], the last
 * one closing back to the first vertex.
 *
 * - Winding is orientation. Counter-clockwise is classic; clockwise is [isInsideOut], solid outside.
 * - Convexity is not required. Self-crossing and self-touching loops are legal.
 * - The starting vertex and the traversal direction are data. [equals] distinguishes both.
 *
 * @constructor Validates the vertex list.
 * @throws DegeneratePolygonException when fewer than 3 vertices are given, a vertex lies at
 *   infinity, or two consecutive vertices coincide at the `SCALE` quantum, last and first included.
 */
public open class Polygon(
    /** The finite [FlatPoint]s the loop passes through, in traversal order. The loop closes implicitly. */
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
     * The edges, one [Segment] per consecutive vertex pair. N vertices give N edges, the last
     * wrapping back to the first vertex.
     *
     * - Adjacent edges share their endpoint instances: `edges[i].end === edges[i + 1].start`.
     */
    public val edges: List<Segment> by cached {
        vertices.indices.map { i -> Segment(vertices[i], vertices[(i + 1) % vertices.size]) }
    }

    /** The open [Polyline] over the same walk, ending back on the first vertex. */
    public val toPolyline: Polyline by cached { Polyline(vertices + vertices.first()) }

    /**
     * The raw signed area of the loop: positive when counter-clockwise, negative when clockwise.
     *
     * - A self-crossing loop's area is algebraic, so a symmetric figure-eight has zero area.
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
     * Whether this polygon is inside-out. An inside-out polygon winds clockwise, and its solid
     * region is the closed complement of the loop.
     *
     * - Reads the raw [signedArea], so a zero-area self-cancelling loop is not inside-out.
     */
    public val isInsideOut: Boolean get() = signedArea < 0.0

    /**
     * The area centroid, the balance point of the enclosed region.
     *
     * - Falls back to the vertex mean when [signedArea] rounds to zero.
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

    /**
     * Whether this polygon is convex. True when every vertex turns the same way and the loop winds once.
     *
     * - Either winding qualifies, and so do collinear vertices. A self-crossing or flat loop never does.
     * - A vertex counts as collinear when its turn's sine rounds to zero at the `SCALE` quantum.
     */
    public val isConvex: Boolean by cached {
        val n = vertices.size
        var side = 0.0
        var turning = 0.0
        for (i in 0 until n) {
            val a = vertices[i]
            val b = vertices[(i + 1) % n]
            val c = vertices[(i + 2) % n]
            val ux = b.px - a.px
            val uy = b.py - a.py
            val vx = c.px - b.px
            val vy = c.py - b.py
            val cross = ux * vy - uy * vx
            val sine = round(cross / (hypot(ux, uy) * hypot(vx, vy)))
            if (sine * side < 0.0) return@cached false
            if (sine != 0.0) side = sine
            turning += atan2(cross, ux * vx + uy * vy)
        }
        // |Σ turns| = 2π·|turning number|: a pentagram turns the same way at every vertex, but twice around.
        side != 0.0 && round(Angle(abs(turning))) == round(Angle(2 * PI))
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [polygon]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param polygon The replacement geometry.
     * @return [polygon] itself. An override returns its own class rebuilt around [polygon].
     */
    public open fun copy(polygon: Polygon): Polygon = polygon

    /*
    IDENTITY
    */

    /**
     * Compares the vertex lists pairwise, rounded at the `SCALE` quantum.
     *
     * - The starting vertex and the direction count. The same loop entered elsewhere or reversed compares unequal.
     * - Classes must match exactly. A subclass instance never equals a plain [Polygon].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and matching vertices.
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

    /** Returns the vertices at full stored precision. */
    override fun toString(): String = "Polygon(vertices=$vertices)"

    /** Returns the vertices rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Polygon(vertices=${vertices.map { it.toStringRounded() }})"

    /*
    FACTORY
     */

    public companion object {

        /**
         * Builds the axis-aligned rectangle spanning two opposite corners, given in any order.
         *
         * - Always the counter-clockwise quad: `edges[0]` bottom, `[1]` right, `[2]` top, `[3]` left.
         * - Integer indices follow the quad through any motion. `edges[RectSide]` follows the visual side.
         *
         * @param a One corner.
         * @param b The opposite corner.
         * @return The four-vertex polygon.
         * @throws DegeneratePolygonException when a corner lies at infinity, or the corners share a
         *   rounded x or y.
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
         * @throws DegeneratePolygonException when a vertex lies at infinity, or two of the
         *   three coincide at the `SCALE` quantum.
         */
        public fun Triangle(a: FlatPoint, b: FlatPoint, c: FlatPoint): Polygon = Polygon(listOf(a, b, c))
    }
}

/**
 * `-Polygon => Polygon`<br>
 * Returns this polygon turned inside out, by walking the same loop in reverse.
 *
 * - Its solid region is the complement of this polygon's. It compares unequal to the original.
 *
 * @receiver The polygon to turn inside out.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed polygon.
 */
public operator fun <T : Polygon> T.unaryMinus(): T =
    narrowed(copy(Polygon(vertices.reversed())))

/**
 * Returns this polygon cut into [n] convex pieces of equal area that tile it exactly.
 *
 * - Cuts run perpendicular to the longest edge and alternate on ties. A 2:1 rectangle splits into 8 as a 4×2 grid.
 * - The pieces are plain [Polygon]s wound counter-clockwise, in no particular order.
 *
 * @receiver The classic convex polygon to split.
 * @param n The number of pieces; `1` answers the outline itself.
 * @return The [n] pieces.
 * @throws IllegalArgumentException when [n] is below 1.
 * @throws InsideOutPolygonException when this polygon is inside-out.
 * @throws NonConvexPolygonException when this polygon is not convex.
 * @throws DegeneratePolygonException when two vertices of a piece fall within a `SCALE` quantum.
 */
public fun Polygon.split(n: Int = 2): List<Polygon> {
    require(n >= 1) { "split needs at least 1 piece, got $n" }
    if (isInsideOut) throw InsideOutPolygonException("cannot split an inside-out polygon: ${toStringRounded()}")
    if (!isConvex) throw NonConvexPolygonException("cannot split a non-convex polygon: ${toStringRounded()}")
    return Polygon(vertices).bisected(n, previous = null)
}

// ⌈n/2⌉ : ⌊n/2⌋ by area, cut ⟂ the longest edge; among equal edges, one ∥ the previous cut makes cuts alternate.
private fun Polygon.bisected(n: Int, previous: Direction?): List<Polygon> {
    if (n == 1) return listOf(this)
    val axis = cutAxis(previous)
    val target = signedArea * ((n + 1) / 2) / n
    var low = vertices.minOf { it.px * axis.px + it.py * axis.py }
    var high = vertices.maxOf { it.px * axis.px + it.py * axis.py }
    while (true) {
        val mid = low + (high - low) / 2
        if (mid <= low || mid >= high) break
        if (shoelace(clipped(axis, mid, 1.0)) < target) low = mid else high = mid
    }
    val below = Polygon(clipped(axis, low, 1.0))
    val above = Polygon(clipped(axis, low, -1.0))
    return below.bisected((n + 1) / 2, axis) + above.bisected(n / 2, axis)
}

private fun Polygon.cutAxis(previous: Direction?): Direction {
    val sides = vertices.indices.map { i -> vertices[(i + 1) % vertices.size] - vertices[i] }
    val longest = sides.maxOf { round(it.norm) }
    val ties = sides.filter { round(it.norm) == longest }
    val side = previous?.let { p -> ties.firstOrNull {
        round((it.px * p.px + it.py * p.py) / it.norm) == 0.0 }
    } ?: ties.first()
    return Direction(side.px / side.norm, side.py / side.norm)
}

// Sutherland–Hodgman against `sign·(p·axis − offset) ≤ 0`; a crossing rounding onto a kept vertex is dropped.
private fun Polygon.clipped(axis: Direction, offset: Double, sign: Double): List<FlatPoint> {
    val kept = mutableListOf<FlatPoint>()
    fun keep(p: FlatPoint) {
        if (kept.lastOrNull() != p) kept += p
    }
    vertices.forEachIndexed { i, a ->
        val b = vertices[(i + 1) % vertices.size]
        val da = sign * (a.px * axis.px + a.py * axis.py - offset)
        val db = sign * (b.px * axis.px + b.py * axis.py - offset)
        if (da <= 0.0) keep(a)
        if ((da < 0.0 && db > 0.0) || (da > 0.0 && db < 0.0)) {
            val s = da / (da - db)
            keep(FlatPoint(a.px + s * (b.px - a.px), a.py + s * (b.py - a.py)))
        }
    }
    if (kept.size > 1 && kept.first() == kept.last()) kept.removeAt(kept.lastIndex)
    return kept
}

private fun shoelace(loop: List<FlatPoint>): Double {
    var sum = 0.0
    loop.forEachIndexed { i, a ->
        val b = loop[(i + 1) % loop.size]
        sum += a.px * b.py - b.px * a.py
    }
    return sum / 2
}

/**
 * The four visually named sides of a rectangle.
 *
 * - On a fresh [Polygon.Rectangle], each entry's `ordinal` is its edge's index in [Polygon.edges].
 * - `edges[side]` answers by outward direction, so it keeps naming the visual side after mirrors.
 */
public enum class RectSide {
    /** The side facing down, with outward direction `(0, −1)`. */
    BOTTOM,

    /** The side facing right, with outward direction `(1, 0)`. */
    RIGHT,

    /** The side facing up, with outward direction `(0, 1)`. */
    TOP,

    /** The side facing left, with outward direction `(−1, 0)`. */
    LEFT,
}

/**
 * `edges[RectSide] => Segment`<br>
 * Returns the edge that visually faces [side]'s way, whatever the loop's pose or orientation.
 *
 * - Unambiguous below a 45° tilt. At exactly 45°, two edges tie and the pick is arbitrary.
 * - Meant for a rectangle's edges; any other loop answers its most [side]-facing edge.
 *
 * @receiver The edge loop to scan. The names hold only for a rectangle's [Polygon.edges].
 * @param side The visual side whose edge to read.
 * @return The raw edge facing [side]'s way.
 * @throws NoSuchElementException when the list is empty.
 */
public operator fun List<Segment>.get(side: RectSide): Segment {
    // Shoelace over the loop's edges: positive = counter-clockwise walk.
    var doubledArea = 0.0
    for (edge in this) doubledArea += edge.start.ax * edge.end.ay - edge.end.ax * edge.start.ay
    // Carrier normals point inward on a counter-clockwise loop. Flip them outward.
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
