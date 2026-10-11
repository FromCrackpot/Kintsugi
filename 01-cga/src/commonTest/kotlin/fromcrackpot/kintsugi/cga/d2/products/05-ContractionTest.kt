package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContractionTest {

    /*
    Line contract RoundPoint => Dipole
    */

    @Test
    fun `line contracted by a point centers the dipole at the perpendicular foot`() {
        // (1,3) against y = 0: foot (1,0), radius² = −3² = −9.
        val d = Line(0.0, 1.0, 0.0) contract RoundPoint(1.0, 3.0)
        assertEquals(RoundPoint(1.0, 0.0, 1.0, -4.0), d.center)
        assertEquals(-9.0, d.radius2)
    }

    @Test
    fun `point on the line contracts to a zero-radius dipole at itself`() {
        val d = Line(0.0, 1.0, 0.0) contract RoundPoint(3.0, 0.0)
        assertEquals(0.0, d.radius2)
        assertEquals(RoundPoint(3.0, 0.0), d.center)
    }

    @Test
    fun `contracting by a point at infinity collapses the dipole`() {
        assertFailsWith<DegenerateDipoleException> {
            Line(0.0, 1.0, 0.0) contract RoundPoint(1.0, 0.0, 0.0, 0.0)
        }
    }

    /*
    Line contract FlatPoint => RoundPoint
    */

    @Test
    fun `line contracted by a flat point is its running direction at infinity`() {
        // y = 2 runs along (−1, 0); aw carries the pairing gx·py − gy·px = −3.
        val a = Line(0.0, 1.0, -2.0) contract FlatPoint(3.0, 4.0)
        assertEquals(RoundPoint(-1.0, 0.0, 0.0, -3.0), a)
        assertTrue(a.isAtInfinity)
    }

    /*
    Line contract Dipole => RoundPoint
    */

    @Test
    fun `line contracted by a dipole lands where it crosses the perpendicular bisector`() {
        // 3x + 4y = 0 crosses the bisector x = 1 of {(0,0),(2,0)} at (1, −3/4).
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val a = Line(3.0, 4.0, 0.0) contract d
        assertEquals(RoundPoint(1.0, -0.75, 1.0, 0.0), a)
        assertEquals(a.toFlat, Line(3.0, 4.0, 0.0) meet d.cocarrier)
    }

    @Test
    fun `line parallel to the bisector contracts to a point at infinity`() {
        // A vertical line parallels the vertical bisector of a horizontal dipole.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertTrue((Line(1.0, 0.0, 0.0) contract d).isAtInfinity)
    }

    /*
    Circle contract RoundPoint => Dipole
    */

    @Test
    fun `circle contracted by a point carries the radical axis as its carrier`() {
        // (3,0) against the unit circle: inversion (1/3,0), radical axis x = 5/3.
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val d = circle contract RoundPoint(3.0, 0.0)
        assertEquals(Dipole(3.0, 0.0, -5.0, 0.0, -1.5, 0.0), d)
        assertEquals(Line(3.0, 0.0, -5.0), d.carrier)
    }

    @Test
    fun `point on the circle contracts to a zero-radius dipole at itself`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val d = circle contract RoundPoint(1.0, 0.0)
        assertEquals(0.0, d.radius2)
        assertEquals(RoundPoint(1.0, 0.0), d.center)
    }

    @Test
    fun `contracting by the center sends the radical axis to infinity`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue((circle contract RoundPoint(0.0, 0.0)).isAtInfinity)
    }

    /*
    Circle contract FlatPoint => RoundPoint
    */

    @Test
    fun `circle contracted by a flat point is the tangent bearing at infinity`() {
        // At (3,0)'s bearing the unit circle runs along (0, −1); the point's own radial is (1,0).
        val a = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0) contract FlatPoint(3.0, 0.0)
        assertEquals(RoundPoint(0.0, -3.0, 0.0, 0.0), a)
        assertTrue(a.isAtInfinity)
    }

    @Test
    fun `flat point on the circle contracts to the tangent direction there`() {
        val a = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0) contract FlatPoint(0.0, 1.0)
        assertEquals(RoundPoint(1.0, 0.0, 0.0, 0.0), a)
    }

    /*
    Circle contract Dipole => RoundPoint
    */

    @Test
    fun `circle contracted by a dipole lands on the bisector with a vanishing pairing`() {
        // Unit circle against the vertical dipole {(1,0),(1,2)}: result on the bisector y = 1.
        val d = RoundPoint(1.0, 0.0) join RoundPoint(1.0, 2.0)
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val a = circle contract d
        assertEquals(RoundPoint(1.0, 1.0, 1.0, 0.5), a)
        assertEquals(0.0, a dot circle)
    }

    @Test
    fun `circle centered on the carrier contracts to a point at infinity`() {
        val d = RoundPoint(1.0, 0.0) join RoundPoint(1.0, 2.0)
        val circle = Circle(centerX = 1.0, centerY = 5.0, radius = 1.0)
        assertTrue((circle contract d).isAtInfinity)
    }

    /*
    Dipole contract RoundPoint => RoundPoint
    */

    @Test
    fun `dipole contracted by a point is the antidual of the swap-circle expansion`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val point = RoundPoint(0.0, 1.0)
        val swap = point expand d
        // Antidual pairing: (e1,e2,e3,e4) = (cx, cy, −cw, −cz) of the swap-circle.
        assertEquals(RoundPoint(swap.cx, swap.cy, -swap.cw, -swap.cz), d contract point)
        assertEquals(RoundPoint(-0.5, 0.0, 1.0, -0.5), d contract point)
    }

    @Test
    fun `point on the bisector contracts to a point at infinity`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertTrue((d contract RoundPoint(1.0, 1.0)).isAtInfinity)
    }

    /*
    FlatPoint contract RoundPoint => RoundPoint
    */

    @Test
    fun `flat point contracted by a point keeps its position sized by minus the squared distance`() {
        val a = FlatPoint(3.0, 0.0) contract RoundPoint(1.0, 0.0)
        assertEquals(RoundPoint(3.0, 0.0, 1.0, 2.5), a)
        assertEquals(-4.0, a.radius2)
    }

    @Test
    fun `coincident operands contract to the plain point itself`() {
        assertEquals(RoundPoint(3.0, 0.0), FlatPoint(3.0, 0.0) contract RoundPoint(3.0, 0.0))
    }
}
