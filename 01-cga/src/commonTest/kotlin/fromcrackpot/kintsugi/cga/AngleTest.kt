package fromcrackpot.kintsugi.cga

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AngleTest {

    /* LITERALS */

    @Test
    fun `deg literal converts to radians`() {
        assertEquals(PI / 2.0, 90.deg.radians, EPS)
        assertEquals(PI / 8.0, 22.5.deg.radians, EPS)
    }

    @Test
    fun `rad literal is identity`() {
        assertEquals(0.8, 0.8.rad.radians, 0.0)
        assertEquals(2.0, 2.rad.radians, 0.0)
    }

    @Test
    fun `deg and rad literals agree`() {
        assertEquals((PI / 2.0).rad.radians, 90.deg.radians, EPS)
        assertEquals(180.deg.radians, PI.rad.radians, EPS)
    }

    /* PROPS */

    @Test
    fun `degrees accessor inverts the deg literal`() {
        assertEquals(90.0, 90.deg.degrees, EPS)
        assertEquals(180.0, PI.rad.degrees, EPS)
    }

    /* CANONICAL */

    @Test
    fun `canonical wraps into the signed single turn`() {
        assertEquals(90.deg, (-270).deg.canonical)
        assertEquals(180.deg, (-180).deg.canonical)
        assertEquals((-90).deg, 270.deg.canonical)
        assertEquals(0.deg, 360.deg.canonical)
    }

    @Test
    fun `canonical keeps an in-range angle unchanged`() {
        assertEquals((-90).deg, (-90).deg.canonical)
        assertEquals(180.deg, 180.deg.canonical)
        assertEquals(0.rad, 0.rad.canonical)
        assertEquals((-1e-10).rad, (-1e-10).rad.canonical)
    }

    @Test
    fun `canonical is exactly idempotent`() {
        val positive = (-270).deg.canonical
        assertEquals(positive, positive.canonical)
        val negative = 270.deg.canonical
        assertEquals(negative, negative.canonical)
    }

    /* COMPARE */

    @Test
    fun `compares by raw radian value`() {
        assertTrue(90.deg > 45.deg)
        assertTrue(45.deg <= 45.deg)
        assertTrue((-270).deg < 90.deg)
    }

    @Test
    fun `comparison does not wrap`() {
        assertTrue(370.deg > 10.deg)
    }

    @Test
    fun `coerceIn clamps an angle`() {
        assertEquals(30.deg, 45.deg.coerceIn((-30).deg, 30.deg))
        assertEquals((-30).deg, (-45).deg.coerceIn((-30).deg, 30.deg))
        assertEquals(10.deg, 10.deg.coerceIn((-30).deg, 30.deg))
    }

    @Test
    fun `range membership`() {
        assertTrue(20.deg in 10.deg..30.deg)
        assertFalse(40.deg in 10.deg..30.deg)
    }

    /* MATH */

    @Test
    fun `operator arithmetic`() {
        assertEquals(-0.5, (-0.5.rad).radians, 0.0)
        assertEquals(1.5, (1.rad + 0.5.rad).radians, 0.0)
        assertEquals(0.5, (1.rad - 0.5.rad).radians, 0.0)
        assertEquals(1.6, (0.8.rad * 2.0).radians, EPS)
        assertEquals(1.6, (2.0 * 0.8.rad).radians, EPS)
        assertEquals(0.4, (0.8.rad / 2.0).radians, EPS)
    }

    /* ROUND */

    @Test
    fun `round decides identity at the quantum`() {
        assertEquals(0.rad, round(30.deg - 30.deg))
        assertEquals(0.rad, round(1e-9.rad))
        assertEquals(Angle(0.25), round(Angle(0.25)))
    }

    @Test
    fun `round sanitizes signed zero`() {
        assertEquals(0.rad, round((-1e-9).rad))
    }

    @Test
    fun `round lands on the nearest quantum multiple`() {
        assertEquals(Angle(0.0002), round(Angle(0.00019)))
    }

    companion object {
        private const val EPS = 1e-12
    }
}
