package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed

/**
 * An open chain of straight edges through a sequence of vertices. N vertices make N − 1 [edges].
 *
 * - The starting vertex and the traversal direction are data. [equals] distinguishes both.
 * - The first and last vertices may coincide, as in a closed walk. Only consecutive ones may not.
 *
 * @constructor Validates the vertex list.
 * @throws DegeneratePolylineException when fewer than 2 vertices are given, a vertex lies at
 *   infinity, or two consecutive vertices coincide at the `SCALE` quantum.
 */
public open class Polyline(
    /** The finite [FlatPoint]s the chain passes through, in traversal order. */
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
     * The edges, one [Segment] per consecutive vertex pair. N vertices give N − 1 edges, with no wrap.
     *
     * - Adjacent edges share their endpoint instances: `edges[i].end === edges[i + 1].start`.
     */
    public val edges: List<Segment> by cached {
        vertices.zipWithNext { a, b -> Segment(a, b) }
    }

    /**
     * The closed [Polygon] over the same walk. A last vertex that coincides with the first is dropped.
     *
     * @throws DegeneratePolygonException when fewer than 3 vertices remain.
     */
    public val toPolygon: Polygon by cached {
        Polygon(if (vertices.first() == vertices.last()) vertices.dropLast(1) else vertices)
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [polyline]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param polyline The replacement geometry.
     * @return [polyline] itself. An override returns its own class rebuilt around [polyline].
     */
    public open fun copy(polyline: Polyline): Polyline = polyline

    /*
    IDENTITY
    */

    /**
     * Compares the vertex lists pairwise, rounded at the `SCALE` quantum.
     *
     * - The starting vertex and the traversal direction count, so the reversed chain compares unequal.
     * - Classes must match exactly. A subclass instance never equals a plain [Polyline].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and matching vertices.
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

    /** Returns the vertices at full stored precision. */
    override fun toString(): String = "Polyline(vertices=$vertices)"

    /** Returns the vertices rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Polyline(vertices=${vertices.map { it.toStringRounded() }})"
}

/**
 * `-Polyline => Polyline`<br>
 * Returns this chain walked in reverse, from the other end over the same edges.
 *
 * - Compares unequal to the original, unless the chain is a palindrome.
 *
 * @receiver The polyline to reverse.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed polyline.
 */
public operator fun <T : Polyline> T.unaryMinus(): T =
    narrowed(copy(Polyline(vertices.reversed())))
