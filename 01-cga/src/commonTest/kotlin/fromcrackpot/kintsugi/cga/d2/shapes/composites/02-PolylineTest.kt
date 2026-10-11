package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import kotlin.test.*

class PolylineTest {

    /* CTOR */

    @Test
    fun `fewer than 2 vertices throws`() {
        assertFailsWith<DegeneratePolylineException> {
            Polyline(listOf(FlatPoint(0.0, 0.0)))
        }
    }

    @Test
    fun `two vertices form a single-edge chain`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0)))
        assertEquals(1, chain.edges.size)
    }

    @Test
    fun `vertex at infinity throws`() {
        assertFailsWith<DegeneratePolylineException> {
            Polyline(listOf(FlatPoint(0.0, 0.0), Direction(1.0, 0.0), FlatPoint(0.0, 1.0)))
        }
    }

    @Test
    fun `coincident consecutive vertices throw`() {
        assertFailsWith<DegeneratePolylineException> {
            Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(0.0, 0.0), FlatPoint(0.0, 1.0)))
        }
    }

    @Test
    fun `coincident first and last vertices are legal - the chain does not close`() {
        // Unlike Polygon there is no last→first wrap check: a chain may revisit points.
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0), FlatPoint(0.0, 0.0)))
        assertEquals(3, chain.edges.size)
    }

    /* MATH */

    @Test
    fun `edges chain in vertex order without wrapping`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(2, chain.edges.size)
        assertEquals(RoundPoint(0.0, 0.0), chain.edges[0].start)
        assertEquals(RoundPoint(4.0, 0.0), chain.edges[1].start)
        assertEquals(RoundPoint(0.0, 3.0), chain.edges[1].end)
    }

    @Test
    fun `adjacent edges share corner instances by reference`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertSame(chain.edges[0].end, chain.edges[1].start)
    }

    @Test
    fun `toPolygon closes the open chain`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0))),
            chain.toPolygon,
        )
    }

    @Test
    fun `toPolygon drops a duplicated last vertex`() {
        // The common SVG spelling of a closed polyline: first == last. Polygon's closing
        // edge is implicit, so the duplicate is normalized away.
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0), FlatPoint(0.0, 0.0)))
        assertEquals(
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0))),
            chain.toPolygon,
        )
    }

    @Test
    fun `toPolygon on a 2-vertex chain propagates the polygon degeneracy`() {
        // Documented, not guarded: a 2-vertex polyline has no polygon form.
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0)))
        assertFailsWith<DegeneratePolygonException> { chain.toPolygon }
    }

    @Test
    fun `polygon toPolyline is the open walk back onto the first vertex`() {
        val triangle = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val walk = triangle.toPolyline
        assertEquals(4, walk.vertices.size)
        assertEquals(walk.vertices.first(), walk.vertices.last())
        assertEquals(3, walk.edges.size)
        // Round-trips through the dedup normalization.
        assertEquals(triangle, walk.toPolygon)
    }

    /* IDENTITY */

    @Test
    fun `equality is rounded and order-sensitive`() {
        val a = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val b = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        // Starting vertex and traversal direction are data — the reversed walk is a different value.
        val reversed = Polyline(listOf(FlatPoint(0.0, 3.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 0.0)))
        assertNotEquals(a, reversed)
    }

    /* NEGATION */

    @Test
    fun `negation reverses the vertex order`() {
        val a = FlatPoint(0.0, 0.0)
        val b = FlatPoint(4.0, 0.0)
        val c = FlatPoint(0.0, 3.0)
        assertEquals(Polyline(listOf(c, b, a)), -Polyline(listOf(a, b, c)))
    }

    @Test
    fun `negation is an involution`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(chain, -(-chain))
    }

    @Test
    fun `a chain and its negation compare unequal`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertNotEquals(chain, -chain)
    }
}
