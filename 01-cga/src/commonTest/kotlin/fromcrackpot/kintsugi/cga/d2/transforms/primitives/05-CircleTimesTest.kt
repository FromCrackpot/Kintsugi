package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CircleTimesTest {

    /* MOTOR — TRANSLATION */

    @Test
    fun `translating a circle equals the circle at the translated center`() {
        assertEquals(Circle(4.0, 6.0, 5.0), Circle(1.0, 2.0, 5.0) * T(3.0, 4.0))
    }

    @Test
    fun `translating an inside-out circle preserves its orientation`() {
        // The inside-out twin negates every component of Circle(1, 2, 5); the translated twin
        // must equal the negation of the translated classic circle (cz = -13.5 there).
        assertEquals(Circle(-4.0, -6.0, 13.5, 1.0), Circle(-1.0, -2.0, -10.0, 1.0) * T(3.0, 4.0))
    }

    @Test
    fun `translating an imaginary circle preserves its signed radius`() {
        // Classic circle at (1, 2) with radius² = -9: 5 + 2·cz = -9 ⇒ cz = -7.
        val moved = Circle(1.0, 2.0, -7.0, -1.0) * T(3.0, 4.0)
        assertTrue(moved.isImaginary, "translation must preserve imaginariness")
        assertEquals(-9.0, moved.radius2, EPS)
        assertEquals(Circle(4.0, 6.0, -30.5, -1.0), moved)
    }

    @Test
    fun `the inverse displacement returns the original circle`() {
        val c = Circle(1.0, 2.0, 5.0)
        assertEquals(c, c * T(3.0, 4.0) * T(-3.0, -4.0))
    }

    /* MOTOR — ROTATION */

    @Test
    fun `rotating a circle carries its center around the pivot`() {
        // Center (2, 0) rotated 90° CCW about the origin lands on (0, 2); radius unchanged.
        assertEquals(Circle(0.0, 2.0, 1.5), Circle(2.0, 0.0, 1.5) * R(FlatPoint(0.0, 0.0), 90.deg))
    }

    @Test
    fun `moving a circle's center equals the moved circle's center`() {
        val c = Circle(1.0, 2.0, 5.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg) * T(1.5, -0.5)
        assertEquals(c.center * m, (c * m).center)
    }

    @Test
    fun `radius2 and cw are invariant under a motor`() {
        val c = Circle(-1.0, -2.0, -10.0, 1.0) // inside-out
        val m = R(FlatPoint(2.0, -3.0), 37.deg) * T(1.5, -0.5)
        val moved = c * m
        assertEquals(c.radius2, moved.radius2, EPS)
        assertEquals(c.cw, moved.cw, EPS)
    }

    @Test
    fun `circle rotation equals the circle rebuilt from moved generators`() {
        // The sandwich is a wedge outermorphism: moving the spanning dipole and round point and
        // re-joining must give the moved circle, orientation included.
        val d = RoundPoint(1.0, 0.0) join RoundPoint(-1.0, 0.0)
        val a = RoundPoint(0.0, 1.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg) * T(1.5, -0.5)
        assertEquals((d * m) join (a * m), (d join a) * m)
    }

    /* FLECTOR */

    @Test
    fun `reflecting a circle carries its center across the mirror and flips cw`() {
        // Center (0, 0), r = 1 reflected across x = 1: center (2, 0), r = 1 — and the improper
        // motion reverses orientation: the classic circle comes back inside-out.
        val c = Circle(0.0, 0.0, 1.0) // classic: (0, 0, 0.5, −1)
        val reflected = c * F(Line(1.0, 0.0, -1.0))
        assertEquals(Circle(-2.0, 0.0, 1.5, 1.0), reflected) // negated classic (2, 0, −1.5, −1)
        assertEquals(c.radius2, reflected.radius2, EPS)
        assertEquals(-c.cw, reflected.cw, EPS)
    }

    @Test
    fun `reflecting a circle's center equals the reflected circle's center`() {
        // center is negation-blind (even in the circle components), so it transports cleanly
        // through the orientation flip.
        val c = Circle(1.0, 2.0, 5.0)
        val f = G(Line(1.0, -1.0, 2.0), 2.0)
        assertEquals(c.center * f, (c * f).center)
    }

    @Test
    fun `radius2 is invariant and cw flips under a flector`() {
        val c = Circle(1.0, 2.0, -7.0, -1.0) // imaginary classic
        val f = G(Line(1.0, -1.0, 2.0), 2.0)
        val reflected = c * f
        assertTrue(reflected.isImaginary, "reflection must preserve imaginariness")
        assertEquals(c.radius2, reflected.radius2, EPS)
        assertEquals(-c.cw, reflected.cw, EPS)
    }

    @Test
    fun `circle reflection equals the circle rebuilt from reflected generators`() {
        val d = RoundPoint(1.0, 0.0) join RoundPoint(-1.0, 0.0)
        val a = RoundPoint(0.0, 1.0)
        val f = G(Line(1.0, -1.0, 2.0), 2.0)
        assertEquals((d * f) join (a * f), (d join a) * f)
    }

    @Test
    fun `reflecting twice across the same line returns the original circle`() {
        val c = Circle(1.0, 2.0, 5.0)
        val f = F(Line(1.0, 2.0, -1.0))
        assertEquals(c, c * f * f)
    }

    companion object {
        private const val EPS = 1e-12
    }
}
