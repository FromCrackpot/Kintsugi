package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.REACH
import fromcrackpot.kintsugi.cga.angle
import fromcrackpot.kintsugi.cga.assertSameShape
import fromcrackpot.kintsugi.cga.degenerate
import fromcrackpot.kintsugi.cga.dilator
import fromcrackpot.kintsugi.cga.flatPoint
import fromcrackpot.kintsugi.cga.sweep
import fromcrackpot.kintsugi.cga.versor
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.unaryMinus
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CircleTimesTest {

    /* MOTOR: TRANSLATION */

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

    /* MOTOR: ROTATION */

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

    /* FLECTOR */

    @Test
    fun `reflecting a circle carries its center across the mirror and flips cw`() {
        // Center (0, 0), r = 1 reflected across x = 1 gives center (2, 0), r = 1. The improper
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
    fun `reflecting twice across the same line returns the original circle`() {
        val c = Circle(1.0, 2.0, 5.0)
        val f = F(Line(1.0, 2.0, -1.0))
        assertEquals(c, c * f * f)
    }


    /* DILATOR */

    @Test
    fun `dilating a circle scales its center offset and its radius`() {
        // Center (1, 2), radius 5, doubled about the origin: center (2, 4), radius 10.
        assertEquals(Circle(2.0, 4.0, 10.0), Circle(1.0, 2.0, 5.0) * D(FlatPoint(0.0, 0.0), 2.0))
    }

    @Test
    fun `dilating an inside-out circle preserves its orientation`() {
        val c = Circle(1.0, 2.0, 5.0)
        val v = D(FlatPoint(3.0, -1.0), 0.5)
        assertEquals(-(c * v), -c * v)
    }

    @Test
    fun `dilating an imaginary circle scales its signed radius`() {
        // Classic circle at (1, 2) with radius² = −9, tripled about its center: radius² = −81.
        val dilated = Circle(1.0, 2.0, -7.0, -1.0) * D(FlatPoint(1.0, 2.0), 3.0)
        assertTrue(dilated.isImaginary, "dilation must preserve imaginariness")
        assertEquals(-81.0, dilated.radius2, EPS)
    }

    @Test
    fun `dilating a circle's center equals the dilated circle's center`() {
        val c = Circle(1.0, 2.0, 5.0)
        val v = D(FlatPoint(3.0, -1.0), 0.5)
        assertEquals(c.center * v, (c * v).center)
    }

    /* SWEEPS */

    @Test
    fun `a moved circle equals the circle rebuilt from its moved generators`() = sweep {
        val center = flatPoint()
        val r = nextDouble(1.0, REACH / 2)
        val phase = angle().radians
        val (p1, p2, p3) = List(3) { k ->
            val theta = phase + (k + nextDouble(-0.25, 0.25)) * 2.0 * PI / 3
            RoundPoint(center.px + r * cos(theta), center.py + r * sin(theta))
        }
        val d = p1 join p2
        val v = versor()
        val s = dilator()
        if (degenerate()) {
            val t = nextDouble(-1.0, 2.0)
            val a = RoundPoint(p1.ax + t * (p2.ax - p1.ax), p1.ay + t * (p2.ay - p1.ay))
            assertFailsWith<DegenerateCircleException> { d join a }
            assertFailsWith<DegenerateCircleException> { (d * v) join (a * v) }
            assertFailsWith<DegenerateCircleException> { (d * s) join (a * s) }
        } else {
            assertSameShape((d * v) join (p3 * v), (d join p3) * v)
            assertSameShape((d * s) join (p3 * s), (d join p3) * s)
        }
    }

    companion object {
        private const val EPS = 1e-12
    }
}
