package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.assertSameShape
import fromcrackpot.kintsugi.cga.dilator
import fromcrackpot.kintsugi.cga.imaginaryDipole
import fromcrackpot.kintsugi.cga.roundPoint
import fromcrackpot.kintsugi.cga.sweep
import fromcrackpot.kintsugi.cga.versor
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.T
import kotlin.test.Test
import kotlin.test.assertEquals

class DipoleTimesTest {

    // Dipole spanning euclidean endpoints (ax, ay) and (bx, by).
    private fun dipole(ax: Double, ay: Double, bx: Double, by: Double): Dipole =
        FlatPoint(ax, ay).toRound join FlatPoint(bx, by).toRound

    /* MOTOR: TRANSLATION */

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

    /* DILATOR */

    @Test
    fun `dilation multiplies a dipole's radius by the scale`() {
        val d = dipole(1.0, 1.0, 4.0, 5.0)
        assertEquals(d.radius * 3.0, (d * D(FlatPoint(2.0, -3.0), 3.0)).radius, EPS)
    }

    /* SWEEPS */

    @Test
    fun `a moved dipole equals the dipole rebuilt from its moved endpoints`() = sweep {
        val a = roundPoint()
        val b = roundPoint()
        val v = versor()
        val s = dilator()
        assertSameShape((a * v) join (b * v), (a join b) * v)
        assertSameShape((a * s) join (b * s), (a join b) * s)
    }

    @Test
    fun `a moved imaginary dipole equals the rebuild from its moved endpoints`() = sweep {
        val d = imaginaryDipole()
        val v = versor()
        val s = dilator()
        assertSameShape((d.start * v) join (d.end * v), d * v)
        assertSameShape((d.start * s) join (d.end * s), d * s)
    }

    companion object {
        private const val EPS = 1e-9
    }
}
