package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineTimesTest {

    // Dipole spanning euclidean endpoints (ax, ay) and (bx, by).
    private fun dipole(ax: Double, ay: Double, bx: Double, by: Double): Dipole =
        FlatPoint(ax, ay).toRound join FlatPoint(bx, by).toRound

    /* MOTOR — TRANSLATION */

    @Test
    fun `translating a line shifts its moment along the normal`() {
        // Line(1, 0, -3) is x = 3; shifting by (2, 0) lands on x = 5.
        assertEquals(Line(1.0, 0.0, -5.0), Line(1.0, 0.0, -3.0) * T(2.0, 0.0))
    }

    @Test
    fun `translating a line along itself is the identity`() {
        val line = Line(1.0, 0.0, -3.0)
        assertEquals(line, line * T(0.0, 5.0))
    }

    @Test
    fun `translation leaves a line at infinity unchanged`() {
        // gx = gy = 0 kills the moment term: the at-infinity line is a fixed point.
        assertEquals(Line(0.0, 0.0, 5.0), Line(0.0, 0.0, 5.0) * T(3.0, 4.0))
    }

    /* MOTOR — ROTATION */

    @Test
    fun `rotating a line turns its normal with the motion`() {
        // x = 3 rotated 90° CCW about the origin is y = 3, normal turned +x → +y.
        assertEquals(Line(0.0, 1.0, -3.0), Line(1.0, 0.0, -3.0) * R(FlatPoint(0.0, 0.0), 90.deg))
    }

    @Test
    fun `line rotation equals the line rebuilt from rotated points`() {
        // The sandwich commutes with the projective join: moving the two spanning points and
        // re-joining must give the moved line, orientation included.
        val p1 = FlatPoint(1.0, 1.0)
        val p2 = FlatPoint(4.0, 5.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg) * T(1.5, -0.5)
        assertEquals((p1 * m) join (p2 * m), (p1 join p2) * m)
    }

    @Test
    fun `moving a dipole's carrier equals the moved dipole's carrier`() {
        val d = dipole(1.0, 1.0, 4.0, 5.0)
        val m = R(FlatPoint(2.0, -3.0), 37.deg) * T(7.0, -2.0)
        assertEquals(d.carrier * m, (d * m).carrier)
    }

    /* FLECTOR */

    @Test
    fun `reflecting a line across the y-axis mirrors its point set`() {
        // x = 3 reflects to x = −3. The improper grade-3 sign keeps the normal at +x — pinned
        // by the join-rebuild below; the point set is the geometric check here.
        val reflected = Line(1.0, 0.0, -3.0) * F(Line(1.0, 0.0, 0.0))
        assertTrue(FlatPoint(-3.0, 0.0) on reflected)
        assertTrue(FlatPoint(-3.0, 7.0) on reflected)
    }

    @Test
    fun `line reflection equals the line rebuilt from reflected points`() {
        val p1 = FlatPoint(1.0, 1.0)
        val p2 = FlatPoint(4.0, 5.0)
        val f = G(Line(1.0, -1.0, 2.0), 2.0)
        assertEquals((p1 * f) join (p2 * f), (p1 join p2) * f)
    }

    @Test
    fun `line reflection by a pure mirror equals the rebuilt line`() {
        val p1 = FlatPoint(1.0, 1.0)
        val p2 = FlatPoint(4.0, 5.0)
        val f = F(Line(2.0, 1.0, -3.0))
        assertEquals((p1 * f) join (p2 * f), (p1 join p2) * f)
    }

    @Test
    fun `reflecting a dipole's carrier equals the reflected dipole's carrier`() {
        val d = dipole(1.0, 1.0, 4.0, 5.0)
        val f = G(Line(1.0, -1.0, 2.0), 2.0)
        assertEquals(d.carrier * f, (d * f).carrier)
    }

    @Test
    fun `a point on the mirror stays on the reflected line iff it was on the original`() {
        // The mirror ∩ line intersection is a fixed point of the reflection.
        val line = Line(1.0, 0.0, -3.0)   // x = 3
        val mirror = Line(0.0, 1.0, -1.0) // y = 1
        val crossing = FlatPoint(3.0, 1.0)
        assertTrue(crossing on line)
        assertTrue(crossing on (line * F(mirror)))
    }

    @Test
    fun `reflecting twice across the same line returns the original line`() {
        val line = Line(3.0, -1.0, 2.0)
        val f = F(Line(1.0, 2.0, -1.0))
        assertEquals(line, line * f * f)
    }
}
