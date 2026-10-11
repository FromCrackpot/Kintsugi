package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed

/**
 * Open chain of straight edges through a sequence of vertices.
 *
 * The open sibling of [Polygon]: N [vertices] make N − 1 [edges], with no closing edge and no
 * interior. Starting vertex and traversal direction are data — [equals] distinguishes both.
 * The first and last vertices may coincide — a closed walk, detected at the `SCALE` quantum
 * (see [toPolygon]); only consecutive coincident vertices are rejected.
 *
 * @constructor Validates the vertex list and throws on degenerate input.
 * @throws DegeneratePolylineException When fewer than 2 vertices are given, a vertex lies at
 *   infinity, or two consecutive vertices coincide at the `SCALE` quantum.
 */
public open class Polyline(
    /** The points the chain passes through — finite [FlatPoint]s in traversal order. */
    public val vertices: List<FlatPoint>,
) : Composite {

    /*
    CTOR
     */

    init {
        if (vertices.size < 2) {
            throw DegeneratePolylineException("needs at least 2 vertices, got ${vertices.size}")
        }
        vertices.forEachIndexed { i, vertex ->
            if (vertex.isAtInfinity) {
                throw DegeneratePolylineException("vertex $i is at infinity: $vertex")
            }
            if (i < vertices.size - 1 && vertex == vertices[i + 1]) {
                throw DegeneratePolylineException("coincident consecutive vertices at $i: ${vertex.toStringRounded()}")
            }
        }
    }

    /*
    MATH
     */

    /**
     * The edges, one [Segment] per consecutive vertex pair — N − 1 for N vertices, no wrap.
     *
     * Adjacent edges share their endpoint instances: `edges[i].end === edges[i + 1].start`.
     */
    public val edges: List<Segment> by cached {
        vertices.zipWithNext { a, b -> Segment(a, b) }
    }

    /**
     * The closed [Polygon] over the same walk.
     *
     * A last vertex coinciding with the first at the `SCALE` quantum is dropped — the
     * polygon's closing edge is implicit.
     *
     * @throws DegeneratePolygonException When fewer than 3 vertices remain — a 2-vertex
     *   polyline has no polygon form.
     */
    public val toPolygon: Polygon by cached {
        Polygon(if (vertices.first() == vertices.last()) vertices.dropLast(1) else vertices)
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [polyline] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a polyline computes its result as a plain [Polyline] and
     * returns it through this hook. This base implementation returns [polyline] itself; a
     * subclass overrides it to rebuild an instance of its own class around [polyline], and
     * every such operation then comes back as that class. The result's runtime class must
     * equal the receiver's — the operations check it and fail with a named error when a
     * subclass misses the override.
     *
     * @param polyline The replacement geometry.
     * @return [polyline] itself; an override returns its own class rebuilt around [polyline].
     */
    public open fun copy(polyline: Polyline): Polyline = polyline

    /*
    IDENTITY
    */

    /**
     * Rounded identity over the vertex walk: equal when the vertex lists match pairwise at the
     * `SCALE` quantum.
     *
     * Starting vertex and traversal direction both count — the reversed chain compares
     * unequal. Identity is exact-class: a subclass instance and a plain [Polyline] never
     * compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its vertices all match
     *   after rounding.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Polyline
        return vertices == other.vertices
    }

    private val _hashCode by cached { vertices.hashCode() }

    /** Returns the hash of the rounded vertices, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the vertices — raw components, full stored precision. */
    override fun toString(): String = "Polyline(vertices=$vertices)"

    /** Returns the vertices rounded at the `SCALE` quantum — the debug-output convention of every shape. */
    public fun toStringRounded(): String = "Polyline(vertices=${vertices.map { it.toStringRounded() }})"
}

/**
 * `-Polyline => Polyline`<br>
 * Returns this chain walked in reverse — the same edges traversed from the other end.
 *
 * Reverses the vertex order: the first vertex becomes the last and every edge flips its
 * running direction. The vertex instances are kept — only the traversal direction changes —
 * and reversing twice restores the original vertex list exactly. Identity is
 * traversal-sensitive, so a chain and its reversal compare unequal (a palindromic chain being
 * its own reversal). The result is the receiver's own class: the reversed geometry returns
 * through [Polyline.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Polyline] to reverse.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed polyline, as the receiver's class.
 */
public operator fun <T : Polyline> T.unaryMinus(): T =
    narrowed(copy(Polyline(vertices.reversed())))
