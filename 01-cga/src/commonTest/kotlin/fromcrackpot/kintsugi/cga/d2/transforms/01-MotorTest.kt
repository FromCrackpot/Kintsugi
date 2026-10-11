package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateMotorException
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MotorTest {

    /* CTOR */

    @Test
    fun `ctor unitizes by the flat weight norm`() {
        assertEquals(Motor(0.0, 0.0, 1.0, 0.0), Motor(0.0, 0.0, 2.0, 0.0))
        assertEquals(Motor(1.0, 2.0, 0.0, 1.0), Motor(2.0, 4.0, 0.0, 2.0))
    }

    @Test
    fun `ctor throws on zero flat weight norm`() {
        assertFailsWith<DegenerateMotorException> { Motor(1.0, 1.0, 0.0, 0.0) }
    }

    /* FACTORY — R */

    @Test
    fun `R by zero angle is the identity motor`() {
        assertEquals(Motor(0.0, 0.0, 0.0, 1.0), R(FlatPoint(3.0, -4.0), 0.rad))
    }

    @Test
    fun `R by 180 about the origin is the pure e43 versor`() {
        assertEquals(Motor(0.0, 0.0, 1.0, 0.0), R(FlatPoint(0.0, 0.0), 180.deg))
    }

    @Test
    fun `R about a finite center weights the generator by sin half-angle`() {
        val s = sqrt(0.5) // sin(45°)
        assertEquals(Motor(3.0 * s, 4.0 * s, s, s), R(FlatPoint(3.0, 4.0), 90.deg))
    }

    @Test
    fun `R by 180 about an at-infinity center has no unitized form`() {
        // cos(90°) rounds to zero and the e43 part vanishes — zero weight norm.
        assertFailsWith<DegenerateMotorException> { R(Direction(1.0, 0.0), 180.deg) }
    }

    /* FACTORY — T */

    @Test
    fun `T builds the translator versor from the displacement`() {
        // T = 𝟙 + (dx/2)·e42 − (dy/2)·e41 ⇒ mx = −dy/2, my = dx/2, mz = 0, mw = 1.
        assertEquals(Motor(-2.0, 1.5, 0.0, 1.0), T(3.0, 4.0))
    }

    @Test
    fun `T by zero is the identity motor`() {
        assertEquals(Motor(0.0, 0.0, 0.0, 1.0), T(0.0, 0.0))
    }

    @Test
    fun `the T direction overload equals the component form`() {
        assertEquals(T(3.0, 4.0), T(Direction(3.0, 4.0)))
    }
}
