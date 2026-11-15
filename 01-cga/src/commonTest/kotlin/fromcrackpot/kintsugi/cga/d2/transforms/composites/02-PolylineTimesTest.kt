package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PolylineTimesTest {

    /* MOTOR */

    @Test
    fun `translating a polyline maps the vertex chain`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(2.0, 3.0)))
        assertEquals(
            Polyline(listOf(FlatPoint(1.0, -2.0), FlatPoint(3.0, -2.0), FlatPoint(3.0, 1.0))).vertices,
            (chain * T(1.0, -2.0)).vertices,
        )
    }

    @Test
    fun `rotating a polyline maps the vertex chain`() {
        val chain = Polyline(listOf(FlatPoint(1.0, 0.0), FlatPoint(2.0, 0.0)))
        assertEquals(
            Polyline(listOf(FlatPoint(0.0, 1.0), FlatPoint(0.0, 2.0))).vertices,
            (chain * R(FlatPoint(0.0, 0.0), 90.deg)).vertices,
        )
    }

    /* FLECTOR */

    @Test
    fun `reflecting a polyline mirrors every vertex`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 1.0), FlatPoint(2.0, 1.0), FlatPoint(2.0, 3.0)))
        assertEquals(
            Polyline(listOf(FlatPoint(0.0, -1.0), FlatPoint(2.0, -1.0), FlatPoint(2.0, -3.0))).vertices,
            (chain * F(Line(0.0, 1.0, 0.0))).vertices,
        )
    }

    /* DILATOR */

    @Test
    fun `dilating a polyline maps the vertex chain`() {
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(2.0, 2.0)))
        assertEquals(
            Polyline(listOf(FlatPoint(1.0, 1.0), FlatPoint(2.0, 1.0), FlatPoint(2.0, 2.0))).vertices,
            (chain * D(FlatPoint(2.0, 2.0), 0.5)).vertices,
        )
    }

    @Test
    fun `a contraction landing consecutive vertices rounded-coincident throws`() {
        // (0.2, 0) contracts to (4e-5, 0), within half a quantum of the origin.
        val chain = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(0.2, 0.0)))
        assertFailsWith<DegeneratePolylineException> { chain * D(FlatPoint(0.0, 0.0), 2e-4) }
    }
}
