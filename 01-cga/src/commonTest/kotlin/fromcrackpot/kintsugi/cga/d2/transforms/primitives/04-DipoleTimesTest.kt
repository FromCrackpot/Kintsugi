package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals

class DipoleTimesTest {

    // Dipole spanning euclidean endpoints (ax, ay) and (bx, by).
    private fun dipole(ax: Double, ay: Double, bx: Double, by: Double): Dipole =
        FlatPoint(ax, ay).toRound join FlatPoint(bx, by).toRound

    /* MOTOR — TRANSLATION */

    @Test
    fun `translating a dipole equals the dipole of its translated endpoints`() {
        assertEquals(dipole(0.0, 1.0, 2.0, 1.0), dipole(0.0, 0.0, 2.0, 0.0) * T(0.0, 1.0))
        assertEquals(dipole(3.0, 1.0, 5.0, 1.0), dipole(0.0, 0.0, 2.0, 0.0) * T(3.0, 1.0))
    }

    @Test
    fun `translation preserves radius and attitude direction`() {
        val d = dipole(1.0, 1.0, 4.0, 5.0)
        val moved = d * T(7.0, -2.0)
        assertEquals(d.radius, moved.radius, EPS)
        // Full attitude equality fails by design: attitude's aw = −pz is a moment term that
        // shifts with the carrier under translation. Only the direction part is invariant.
        assertEquals(d.attitude.toFlat, moved.attitude.toFlat)
    }

    @Test
    fun `zero translation is the identity for a dipole`() {
        val d = dipole(1.0, 2.0, 3.0, 6.0)
        assertEquals(d, d * T(0.0, 0.0))
    }

    @Test
    fun `the inverse displacement returns the original dipole`() {
        val d = dipole(1.0, 2.0, 3.0, 6.0)
        assertEquals(d, d * T(3.0, 4.0) * T(-3.0, -4.0))
    }

    /* MOTOR — ROTATION */

    @Test
    fun `dipole rotation equals the dipole rebuilt from rotated endpoints`() {
        val a = RoundPoint(1.0, 1.0)
        val b = RoundPoint(4.0, 5.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg)
        assertEquals((a * m) join (b * m), (a join b) * m)
    }

    @Test
    fun `imaginary dipole rotation equals the rebuild from rotated endpoints`() {
        // radius² = −1 (see the containers invariant: start ∧ end reconstructs imaginary blades).
        val d = Dipole(0.0, 1.0, 0.0, -1.0, 0.0, 1.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg)
        assertEquals((d.start * m) join (d.end * m), d * m)
    }

    /* FLECTOR */

    @Test
    fun `dipole reflection equals the dipole rebuilt from reflected endpoints`() {
        val a = RoundPoint(1.0, 1.0)
        val b = RoundPoint(4.0, 5.0)
        val f = G(Line(1.0, -1.0, 0.0), 2.0)
        assertEquals((a * f) join (b * f), (a join b) * f)
    }

    @Test
    fun `dipole reflection by a pure mirror equals the endpoint rebuild`() {
        val a = RoundPoint(1.0, 1.0)
        val b = RoundPoint(4.0, 5.0)
        val f = F(Line(2.0, 1.0, -3.0))
        assertEquals((a * f) join (b * f), (a join b) * f)
    }

    @Test
    fun `imaginary dipole reflection equals the rebuild from reflected endpoints`() {
        // radius² = −1 (see the containers invariant: start ∧ end reconstructs imaginary blades).
        val d = Dipole(0.0, 1.0, 0.0, -1.0, 0.0, 1.0)
        val f = G(Line(1.0, -1.0, 0.0), 2.0)
        assertEquals((d.start * f) join (d.end * f), d * f)
    }

    companion object {
        private const val EPS = 1e-9
    }
}
