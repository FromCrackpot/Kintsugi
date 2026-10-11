package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ExpansionTest {

    /*
    RoundPoint expand Line => Dipole
    */

    @Test
    fun `point expanded onto a line pairs it with its mirror image`() {
        // (0,2) across y = 0 mirrors to (0,-2); expansion equals the join of the pair, orientation included.
        val expected = RoundPoint(0.0, 2.0) join RoundPoint(0.0, -2.0)
        assertEquals(expected, RoundPoint(0.0, 2.0) expand Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `point on the line expands to a zero-radius dipole at itself`() {
        val d = RoundPoint(3.0, 0.0) expand Line(0.0, 1.0, 0.0)
        assertEquals(0.0, d.radius2)
        assertEquals(RoundPoint(3.0, 0.0), d.center)
    }

    @Test
    fun `expanding onto a line at infinity collapses and carries the point itself`() {
        val ex = assertFailsWith<DegenerateDipoleException> {
            RoundPoint(1.0, 1.0) expand Line(0.0, 0.0, 5.0)
        }
        // Payload (-5, -5, -5) re-unitizes to the expanded point.
        assertEquals(FlatPoint(1.0, 1.0), ex.point)
    }

    /*
    RoundPoint expand Circle => Dipole
    */

    @Test
    fun `point expanded onto a circle pairs it with its inversion`() {
        // (3,0) inverts through the unit circle to (1/3,0); expansion equals the join, orientation included.
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val expected = RoundPoint(1.0 / 3.0, 0.0) join RoundPoint(3.0, 0.0)
        assertEquals(expected, RoundPoint(3.0, 0.0) expand circle)
    }

    @Test
    fun `point on the circle expands to a zero-radius dipole at itself`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val d = RoundPoint(1.0, 0.0) expand circle
        assertEquals(0.0, d.radius2)
        assertEquals(RoundPoint(1.0, 0.0), d.center)
    }

    @Test
    fun `flipping the circle inside-out flips the expansion's orientation`() {
        val classic = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val insideOut = Circle(0.0, 0.0, -0.5, 1.0)
        val a = RoundPoint(3.0, 0.0) expand classic
        val b = RoundPoint(3.0, 0.0) expand insideOut
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    @Test
    fun `expanding the center onto its circle collapses the dipole`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertFailsWith<DegenerateDipoleException> { RoundPoint(0.0, 0.0) expand circle }
    }

    /*
    RoundPoint expand Dipole => Circle
    */

    @Test
    fun `point expanded onto a dipole is the circle swapping its endpoints under inversion`() {
        // Through (0,1), swapping (0,0) <-> (2,0): center (-1/2, 0), radius² = 5/4.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val c = RoundPoint(0.0, 1.0) expand d
        assertEquals(Circle(-0.5, 0.0, 0.5, -1.0), c)
        // The receiver lies on the result.
        assertEquals(0.0, RoundPoint(0.0, 1.0) dot c)
    }

    @Test
    fun `point on the endpoints' perpendicular bisector collapses to that mirror line`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val ex = assertFailsWith<DegenerateCircleException> { RoundPoint(1.0, 1.0) expand d }
        // The swap-circle straightens into the bisector x = 1.
        assertEquals(Line(-1.0, 0.0, 1.0), ex.line)
    }

    /*
    RoundPoint expand FlatPoint => Circle
    */

    @Test
    fun `point expanded onto a flat point is the circle through it centered there`() {
        // Through (1,0), centered (3,0): radius 2, built inside-out by the expansion.
        assertEquals(Circle(-3.0, 0.0, 2.5, 1.0), RoundPoint(1.0, 0.0) expand FlatPoint(3.0, 0.0))
    }

    @Test
    fun `expanding onto a center at infinity collapses to the perpendicular line payload`() {
        val ex = assertFailsWith<DegenerateCircleException> {
            RoundPoint(1.0, 0.0) expand Direction(0.0, 1.0)
        }
        // The line through (1,0) perpendicular to the direction (0,1).
        assertEquals(Line(0.0, -1.0, 0.0), ex.line)
    }

    /*
    Dipole expand Line => Circle
    */

    @Test
    fun `dipole expanded onto a line is the circle through its endpoints centered on the line`() {
        // Endpoints (0,0), (2,0); line y = 3 → circle centered (1,3), radius² = 10, inside-out.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertEquals(Circle(-1.0, -3.0, 0.0, 1.0), d expand Line(0.0, 1.0, -3.0))
    }

    @Test
    fun `carrier perpendicular to the line collapses to the carrier payload`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val ex = assertFailsWith<DegenerateCircleException> { d expand Line(1.0, 0.0, 0.0) }
        assertEquals(Line(0.0, 1.0, 0.0), ex.line)
    }

    /*
    Dipole expand Circle => Circle
    */

    @Test
    fun `dipole expanded onto a circle is the circle through its endpoints orthogonal to it`() {
        // Endpoints (0,0), (2,0); circle centered (1,2) radius 1 → result centered (1, 1/2), radius² = 5/4.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 1.0, centerY = 2.0, radius = 1.0)
        val c = d expand circle
        assertEquals(Circle(-1.0, -0.5, 0.0, 1.0), c)
        // Orthogonal circles dot to zero.
        assertEquals(0.0, c dot circle)
    }

    @Test
    fun `circle centered on the carrier collapses to the carrier payload`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 1.0, centerY = 0.0, radius = 1.0)
        val ex = assertFailsWith<DegenerateCircleException> { d expand circle }
        assertEquals(Line(0.0, 1.0, 0.0), ex.line)
    }

    /*
    FlatPoint expand Line => Line
    */

    @Test
    fun `flat point expanded onto a line is the perpendicular through it`() {
        // Through (3,4), perpendicular to y = 2 → the vertical x = 3.
        assertEquals(Line(1.0, 0.0, -3.0), FlatPoint(3.0, 4.0) expand Line(0.0, 1.0, -2.0))
    }

    @Test
    fun `flat point expanded onto a line at infinity collapses to the zero line`() {
        assertEquals(Line(0.0, 0.0, 0.0), FlatPoint(3.0, 4.0) expand Line(0.0, 0.0, 5.0))
    }

    @Test
    fun `direction expanded onto a line is a line at infinity kept raw`() {
        assertEquals(Line(0.0, 0.0, -1.0), Direction(1.0, 0.0) expand Line(0.0, 1.0, 0.0))
    }

    /*
    FlatPoint expand Circle => Line
    */

    @Test
    fun `flat point expanded onto a circle is the radial line through it`() {
        // Through (3,0) and the unit circle's center: y = 0.
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(Line(0.0, 1.0, 0.0), FlatPoint(3.0, 0.0) expand circle)
    }

    @Test
    fun `flat point at the circle's center collapses to the zero line`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(Line(0.0, 0.0, 0.0), FlatPoint(0.0, 0.0) expand circle)
    }
}
