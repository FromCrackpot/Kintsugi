package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarTest {

    /*
    DIRECTION (at infinity)
     */

    @Test
    fun `scales a direction's magnitude and stays at infinity`() {
        val v = Direction(3.0, 4.0) * 2.0
        assertEquals(Direction(6.0, 8.0), v)
        assertTrue(v.isAtInfinity, "scaling must preserve pz = 0")
    }

    @Test
    fun `a negative scalar flips a direction's orientation`() {
        assertEquals(Direction(-3.0, -4.0), Direction(3.0, 4.0) * -1.0)
    }

    /*
    FINITE POINT (homogeneous no-op)
     */

    @Test
    fun `a unitized finite point is a fixed point of scaling`() {
        val p = FlatPoint(1.0, 2.0)
        assertEquals(p, p * 5.0)
        assertEquals(p, p * -2.0) // pz < 0 re-unitizes
    }

    /*
    INTEROP
     */

    @Test
    fun `T of a scaled velocity translates by the scaled displacement`() {
        // The scaled-velocity idiom: `p * T(v * dt)` — the homogeneous FlatPoint * Double scales
        // the free vector before it becomes the translator's generator.
        val p = FlatPoint(1.0, 2.0)
        val v = Direction(30.0, 40.0)
        assertEquals(FlatPoint(4.0, 6.0), p * T(v * 0.1))
    }
}
