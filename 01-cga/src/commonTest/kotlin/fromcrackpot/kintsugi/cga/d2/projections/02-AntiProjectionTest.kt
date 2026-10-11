package fromcrackpot.kintsugi.cga.d2.projections

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.products.contract
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.products.join
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AntiProjectionTest {

    /*
    Line antiproject FlatPoint => Line
    */

    @Test
    fun `line antiprojects onto a flat point as the parallel through it`() {
        assertEquals(Line(0.0, 1.0, -4.0), Line(0.0, 1.0, 0.0) antiproject FlatPoint(3.0, 4.0))
    }

    @Test
    fun `point on the line antiprojects the line onto itself`() {
        assertEquals(Line(0.0, 1.0, 0.0), Line(0.0, 1.0, 0.0) antiproject FlatPoint(3.0, 0.0))
    }

    @Test
    fun `antiprojection negates the join of the contraction`() {
        // The inner product is the weight contraction −(g contract p): the compositional
        // spelling comes back orientation-flipped, same point set.
        val g = Line(0.0, 1.0, 0.0)
        val p = FlatPoint(3.0, 4.0)
        val composed = p join (g contract p)
        val fused = g antiproject p
        assertNotEquals(fused, composed)
        assertEquals(fused.canonical, composed.canonical)
    }

    /*
    Line antiproject RoundPoint (forwarder)
    */

    @Test
    fun `round target forwards to the flat form on its location`() {
        val g = Line(0.0, 1.0, 0.0)
        assertEquals(g antiproject FlatPoint(3.0, 4.0), g antiproject RoundPoint(3.0, 4.0))
    }

    @Test
    fun `a sized round target antiprojects by location alone`() {
        val g = Line(0.0, 1.0, 0.0)
        val sized = RoundPoint(3.0, 4.0, 1.0, 99.0)
        assertEquals(g antiproject RoundPoint(3.0, 4.0), g antiproject sized)
    }

    /*
    Line antiproject Dipole => Circle
    */

    @Test
    fun `line perpendicular to the carrier bends into the endpoint-diameter circle`() {
        // x = 0 against {(0,0),(2,0)}: the circle centered (1,0) with the endpoints as diameter.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertEquals(Circle(1.0, 0.0, 0.0, -1.0), Line(1.0, 0.0, 0.0) antiproject d)
    }

    @Test
    fun `line antiprojects onto a dipole as a circle through its endpoints`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val c = Line(0.0, 1.0, -5.0) antiproject d
        assertEquals(Circle(-1.0, 0.2, 0.0, 1.0), c)
        // Both endpoints are incident with the bent circle.
        assertEquals(0.0, RoundPoint(0.0, 0.0) dot c)
        assertEquals(0.0, RoundPoint(2.0, 0.0) dot c)
    }

    @Test
    fun `line parallel to the carrier collapses to the carrier payload`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val ex = assertFailsWith<DegenerateCircleException> { Line(0.0, 1.0, 0.0) antiproject d }
        assertEquals(Line(0.0, 1.0, 0.0), ex.line)
    }

    @Test
    fun `dipole antiprojection negates the join of the contraction`() {
        val g = Line(1.0, 0.0, 0.0)
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val composed = d join (g contract d)
        val fused = g antiproject d
        assertNotEquals(fused, composed)
        assertEquals(fused.canonical, composed.canonical)
    }

    /*
    Circle antiproject FlatPoint => Line
    */

    @Test
    fun `circle antiprojects onto a flat point as the radial-perpendicular through it`() {
        // At (3,0)'s bearing: the vertical x = 3, normal pointing back at the center.
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(Line(-1.0, 0.0, 3.0), circle antiproject FlatPoint(3.0, 0.0))
    }

    @Test
    fun `point on the circle antiprojects to the inward-oriented tangent`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(Line(0.0, -1.0, 1.0), circle antiproject FlatPoint(0.0, 1.0))
    }

    @Test
    fun `flat point at the circle's center antiprojects to the zero line`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(Line(0.0, 0.0, 0.0), circle antiproject FlatPoint(0.0, 0.0))
    }

    /*
    Circle antiproject RoundPoint (forwarder)
    */

    @Test
    fun `round target forwards to the flat form for circles too`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(circle antiproject FlatPoint(3.0, 0.0), circle antiproject RoundPoint(3.0, 0.0))
    }

    /*
    Circle antiproject Dipole => Circle
    */

    @Test
    fun `circle through the endpoints antiprojects to itself`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 1.0, centerY = 1.0, radius = kotlin.math.sqrt(2.0))
        assertEquals(circle, circle antiproject d)
    }

    @Test
    fun `circle antiprojects onto a dipole as a circle through its endpoints`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val c = circle antiproject d
        assertEquals(Circle(1.0, 0.0, 0.0, -1.0), c)
        assertEquals(0.0, RoundPoint(0.0, 0.0) dot c)
        assertEquals(0.0, RoundPoint(2.0, 0.0) dot c)
    }

    @Test
    fun `circle dipole antiprojection negates the join of the contraction`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val composed = d join (circle contract d)
        val fused = circle antiproject d
        assertNotEquals(fused, composed)
        assertEquals(fused.canonical, composed.canonical)
    }

    @Test
    fun `collapsing round weight throws with the straightened line payload`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = -1.0, centerY = 0.0, radius = kotlin.math.sqrt(3.0))
        assertFailsWith<DegenerateCircleException> { circle antiproject d }
    }
}
