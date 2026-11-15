package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.assertSameShape
import fromcrackpot.kintsugi.cga.dilator
import fromcrackpot.kintsugi.cga.dipole
import fromcrackpot.kintsugi.cga.flatPoint
import fromcrackpot.kintsugi.cga.sweep
import fromcrackpot.kintsugi.cga.versor
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.operations.queries.predicates.on
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineTimesTest {

    /* MOTOR: TRANSLATION */

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

    /* MOTOR: ROTATION */

    @Test
    fun `rotating a line turns its normal with the motion`() {
        // x = 3 rotated 90° CCW about the origin is y = 3, normal turned +x → +y.
        assertEquals(Line(0.0, 1.0, -3.0), Line(1.0, 0.0, -3.0) * R(FlatPoint(0.0, 0.0), 90.deg))
    }

    /* FLECTOR */

    @Test
    fun `reflecting a line across the y-axis mirrors its point set`() {
        // x = 3 reflects to x = −3. The improper grade-3 sign keeps the normal at +x, as pinned
        // by the join-rebuild sweep below; the point set is the geometric check here.
        val reflected = Line(1.0, 0.0, -3.0) * F(Line(1.0, 0.0, 0.0))
        assertTrue(FlatPoint(-3.0, 0.0) on reflected)
        assertTrue(FlatPoint(-3.0, 7.0) on reflected)
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

    /* DILATOR */

    @Test
    fun `dilating a line scales its distance from the center`() {
        // x = 3 doubled about the origin is x = 6.
        assertEquals(Line(1.0, 0.0, -6.0), Line(1.0, 0.0, -3.0) * D(FlatPoint(0.0, 0.0), 2.0))
    }

    @Test
    fun `dilation fixes a line through its center`() {
        val c = FlatPoint(1.0, 2.0)
        val line = c join FlatPoint(4.0, -1.0)
        assertEquals(line, line * D(c, 3.0))
    }

    @Test
    fun `dilation scales the moment of a line at infinity`() {
        assertEquals(Line(0.0, 0.0, 10.0), Line(0.0, 0.0, 5.0) * D(FlatPoint(3.0, 4.0), 2.0))
    }

    /* SWEEPS */

    @Test
    fun `a moved line equals the line rebuilt from its moved points`() = sweep {
        val p1 = flatPoint()
        val p2 = flatPoint()
        val v = versor()
        val s = dilator()
        assertSameShape((p1 * v) join (p2 * v), (p1 join p2) * v)
        assertSameShape((p1 * s) join (p2 * s), (p1 join p2) * s)
    }

    @Test
    fun `a moved dipole's carrier equals the moved carrier`() = sweep {
        val d = dipole()
        val v = versor()
        val s = dilator()
        assertSameShape(d.carrier * v, (d * v).carrier)
        assertSameShape(d.carrier * s, (d * s).carrier)
    }
}
