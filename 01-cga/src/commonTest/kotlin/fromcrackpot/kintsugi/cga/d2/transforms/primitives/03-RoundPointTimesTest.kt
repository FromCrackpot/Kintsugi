package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundPointTimesTest {

    /* MOTOR — TRANSLATION */

    @Test
    fun `translating a round point equals the lift of the translated euclidean point`() {
        assertEquals(RoundPoint(4.0, 6.0), RoundPoint(1.0, 2.0) * T(3.0, 4.0))
    }

    @Test
    fun `translating a non-lift round point preserves its radius`() {
        // aw = lift + radius²/2 ⇒ radius2 = 9.0 (a real round point of radius 3 at (1, 2)).
        val rp = RoundPoint(1.0, 2.0, 1.0, (1.0 + 4.0) / 2.0 + 4.5)
        val moved = rp * T(3.0, 4.0)
        // Expected aw carries the true post-transform value: lift of (4, 6) + the same radius²/2.
        assertEquals(RoundPoint(4.0, 6.0, 1.0, (16.0 + 36.0) / 2.0 + 4.5), moved)
        assertEquals(9.0, moved.radius2, EPS)
    }

    @Test
    fun `an at-infinity round point keeps its position and gains only the moment term`() {
        // az = 0 kills the displacement; aw' = aw + tx·ax + ty·ay — the same moment shift
        // Dipole.attitude exhibits under translation.
        assertEquals(RoundPoint(1.0, 0.0, 0.0, 0.5 + 5.0), RoundPoint(1.0, 0.0, 0.0, 0.5) * T(5.0, 7.0))
    }

    @Test
    fun `zero translation is the identity for a round point`() {
        val rp = RoundPoint(1.0, 2.0, 1.0, 7.0)
        assertEquals(rp, rp * T(0.0, 0.0))
    }

    @Test
    fun `the inverse displacement returns the original round point`() {
        val rp = RoundPoint(1.0, 2.0, 1.0, 7.0)
        assertEquals(rp, rp * T(3.0, 4.0) * T(-3.0, -4.0))
    }

    /* MOTOR — ROTATION */

    @Test
    fun `round point rotation agrees with the flat point action on lifts`() {
        val m = R(FlatPoint(1.0, -1.0), 60.deg)
        assertEquals((FlatPoint(3.0, 2.0) * m).toRound, RoundPoint(3.0, 2.0) * m)
    }

    @Test
    fun `round point radius2 is invariant under rotation`() {
        // Non-lift aw (radius2 = 5) — rigid motions preserve the radius, only the center moves.
        val a = RoundPoint(1.0, 2.0, 1.0, 5.0)
        val m = R(FlatPoint(-2.0, 3.0), 37.deg)
        assertEquals(a.radius2, (a * m).radius2, EPS)
    }

    /* FLECTOR */

    @Test
    fun `reflecting across the x-axis mirrors a round point`() {
        assertEquals(RoundPoint(1.0, -2.0), RoundPoint(1.0, 2.0) * F(Line(0.0, 1.0, 0.0)))
    }

    @Test
    fun `round point reflection agrees with the flat point action on lifts`() {
        val f = G(Line(1.0, -1.0, 0.0), 2.0)
        assertEquals((FlatPoint(3.0, 2.0) * f).toRound, RoundPoint(3.0, 2.0) * f)
    }

    @Test
    fun `round point radius2 is invariant under reflection`() {
        // Non-lift aw (radius2 = 9) — an isometry preserves the radius, only the center moves.
        val a = RoundPoint(1.0, 2.0, 1.0, (1.0 + 4.0) / 2.0 + 4.5)
        val f = G(Line(1.0, -1.0, 0.0), 2.0)
        assertEquals(9.0, a.radius2, EPS)
        assertEquals(9.0, (a * f).radius2, EPS)
    }

    companion object {
        private const val EPS = 1e-12
    }
}
