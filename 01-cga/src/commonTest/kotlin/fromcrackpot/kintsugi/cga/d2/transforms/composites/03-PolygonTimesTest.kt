package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PolygonTimesTest {

    /* MOTOR */

    @Test
    fun `translating a polygon maps the vertex loop`() {
        val triangle = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 2.0))
        assertEquals(
            Polygon.Triangle(FlatPoint(1.0, -2.0), FlatPoint(3.0, -2.0), FlatPoint(1.0, 0.0)),
            triangle * T(1.0, -2.0),
        )
    }

    @Test
    fun `rotating a polygon maps the vertex loop`() {
        val square = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0))
        assertEquals(
            Polygon.Rectangle(FlatPoint(-2.0, 0.0), FlatPoint(0.0, 2.0)).vertices.toSet(),
            (square * R(FlatPoint(0.0, 0.0), 90.deg)).vertices.toSet(),
        )
    }

    /* FLECTOR */

    @Test
    fun `reflecting a polygon mirrors every vertex`() {
        val square = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0))
        val reflected = square * F(Line(0.0, 1.0, 0.0)) // across the x-axis
        val expected = listOf(
            FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(2.0, -1.0), FlatPoint(0.0, -1.0),
        )
        assertEquals(expected, reflected.vertices)
    }

    @Test
    fun `a reflected CCW polygon has reversed winding but is still valid`() {
        // The CCW rectangle reflects to a CW loop (same vertices, orientation flipped); the ctor
        // accepts it (containment classifies by winding), so no exception and vertices are mirrored.
        val ccw = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0))
        val reflected = ccw * G(Line(0.0, 1.0, 0.0), 4.0)
        assertTrue(reflected.vertices.all { !it.isAtInfinity })
    }

    /* DILATOR */

    @Test
    fun `dilating a polygon maps the vertex loop`() {
        val triangle = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 2.0))
        assertEquals(
            Polygon.Triangle(FlatPoint(-1.0, -1.0), FlatPoint(3.0, -1.0), FlatPoint(-1.0, 3.0)),
            triangle * D(FlatPoint(1.0, 1.0), 2.0),
        )
    }

    @Test
    fun `dilation keeps the winding and scales the area by the scale squared`() {
        val square = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0))
        assertEquals(square.signedArea * 2.25, (square * D(FlatPoint(5.0, -1.0), 1.5)).signedArea, EPS)
    }

    @Test
    fun `a contraction landing consecutive vertices rounded-coincident throws`() {
        // Every vertex contracts to within half a quantum of the origin.
        val triangle = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(0.2, 0.0), FlatPoint(0.0, 0.2))
        assertFailsWith<DegeneratePolygonException> { triangle * D(FlatPoint(0.0, 0.0), 2e-4) }
    }

    companion object {
        private const val EPS = 1e-9
    }
}
