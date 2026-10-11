package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.products.join
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class IncidenceTest {

    /*
    FlatPoint <on> Line
    */

    @Test
    fun `point on line returns true`() {
        assertTrue(FlatPoint(5.0, 0.0) on Line(0.0, 1.0, 0.0))   // (5, 0) on y = 0
    }

    @Test
    fun `point off line returns false`() {
        assertFalse(FlatPoint(5.0, 3.0) on Line(0.0, 1.0, 0.0))  // (5, 3) not on y = 0
    }

    @Test
    fun `joined line is incident on both endpoints`() {
        // a join b produces the line through a and b — both endpoints satisfy `on`.
        val a = FlatPoint(1.0, 1.0)
        val b = FlatPoint(4.0, 5.0)
        val l = a join b
        assertTrue(a on l)
        assertTrue(b on l)
    }

    /*
    RoundPoint <on> Line
    */

    @Test
    fun `round point on line returns true`() {
        assertTrue(RoundPoint(5.0, 0.0) on Line(0.0, 1.0, 0.0))   // (5, 0) on y = 0
    }

    @Test
    fun `round point off line returns false`() {
        assertFalse(RoundPoint(5.0, 3.0) on Line(0.0, 1.0, 0.0))  // (5, 3) not on y = 0
    }

    /*
    FlatPoint <on> Circle / RoundPoint <on> Circle
    */

    @Test
    fun `flat point on classic circle boundary returns true`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(FlatPoint(1.0, 0.0) on c)
        assertTrue(FlatPoint(0.0, 1.0) on c)
        assertTrue(FlatPoint(-1.0, 0.0) on c)
    }

    @Test
    fun `flat point strictly inside or outside classic circle returns false`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertFalse(FlatPoint(0.5, 0.0) on c)
        assertFalse(FlatPoint(2.0, 0.0) on c)
    }

    @Test
    fun `on is orientation-blind (inside-out circle)`() {
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0) // unit circle, inside-out
        assertTrue(FlatPoint(1.0, 0.0) on insideOut)
        assertFalse(FlatPoint(0.5, 0.0) on insideOut)
    }

    @Test
    fun `round point on classic circle boundary returns true`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(RoundPoint(1.0, 0.0) on c)
        assertFalse(RoundPoint(0.5, 0.0) on c)
    }

    /*
    FlatPoint <on> Segment
    */

    @Test
    fun `interior point on segment returns true`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertTrue(FlatPoint(0.5, 1.0) on s)
    }

    @Test
    fun `segment endpoints count as on (inclusive)`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertTrue(FlatPoint(-2.0, 1.0) on s)
        assertTrue(FlatPoint(2.0, 1.0) on s)
    }

    @Test
    fun `carrier point beyond an endpoint is not on the segment`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertFalse(FlatPoint(3.0, 1.0) on s)    // past end
        assertFalse(FlatPoint(-2.5, 1.0) on s)   // before start
    }

    @Test
    fun `point off the carrier is not on the segment`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertFalse(FlatPoint(0.0, 2.0) on s)    // above
        assertFalse(FlatPoint(0.0, 0.0) on s)    // below
    }

    @Test
    fun `on segment works on a diagonal segment`() {
        val s = Segment(FlatPoint(0.0, 0.0), FlatPoint(4.0, 4.0))
        assertTrue(FlatPoint(2.0, 2.0) on s)
        assertFalse(FlatPoint(5.0, 5.0) on s)    // collinear, past end
        assertFalse(FlatPoint(2.0, 3.0) on s)    // off carrier
    }

    @Test
    fun `on segment is independent of endpoint order`() {
        // carrier/cocarrier orientation flips with the wedge order; abs keeps the result stable.
        val mid = FlatPoint(0.0, 1.0)
        val past = FlatPoint(3.0, 1.0)
        assertTrue(mid on Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0)))
        assertTrue(mid on Segment(FlatPoint(2.0, 1.0), FlatPoint(-2.0, 1.0)))
        assertFalse(past on Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0)))
        assertFalse(past on Segment(FlatPoint(2.0, 1.0), FlatPoint(-2.0, 1.0)))
    }

    @Test
    fun `on segment stays robust at large coordinates`() {
        // Ports the retired `between` robustness pin: at coords ~10⁷ a quadratic route
        // (aw ≈ 5·10¹³ before subtraction) loses sub-SCALE detail; the two all-linear
        // carrier/cocarrier gates must keep it.
        val s = Segment(FlatPoint(1e7, 0.0), FlatPoint(1e7 + 4.0, 0.0))
        assertTrue(FlatPoint(1e7 + 2.0, 0.0) on s)
        assertFalse(FlatPoint(1e7 + 5.0, 0.0) on s)
    }

    /*
    RoundPoint <on> Segment
    */

    @Test
    fun `round point interior and endpoints are on the segment (inclusive)`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertTrue(RoundPoint(0.5, 1.0) on s)
        assertTrue(RoundPoint(-2.0, 1.0) on s)
        assertTrue(RoundPoint(2.0, 1.0) on s)
    }

    @Test
    fun `round point beyond an endpoint or off the carrier is not on the segment`() {
        val s = Segment(FlatPoint(-2.0, 1.0), FlatPoint(2.0, 1.0))
        assertFalse(RoundPoint(3.0, 1.0) on s)    // collinear, past end
        assertFalse(RoundPoint(0.0, 2.0) on s)    // off carrier
    }

    /*
    FlatPoint <on> Polygon
    */

    // Off-origin CCW quad, corners (1, 2) .. (5, 5) — matches the rectangle fixture.
    private val quad = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))

    @Test
    fun `point on a polygon edge is on the polygon`() {
        assertTrue(FlatPoint(3.0, 2.0) on quad)   // bottom edge interior
        assertTrue(FlatPoint(1.0, 2.0) on quad)   // vertex
        assertFalse(FlatPoint(3.0, 4.0) on quad)  // strict interior
        assertFalse(FlatPoint(6.0, 4.0) on quad)  // outside
    }

    /*
    RoundPoint <on> Polygon
    */

    @Test
    fun `round point on a polygon edge is on the polygon`() {
        assertTrue(RoundPoint(3.0, 2.0) on quad)   // bottom edge interior
        assertTrue(RoundPoint(1.0, 2.0) on quad)   // vertex
        assertFalse(RoundPoint(3.0, 4.0) on quad)  // strict interior
        assertFalse(RoundPoint(6.0, 4.0) on quad)  // outside
    }
}
