package fromcrackpot.kintsugi.scenegraph

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SceneBoundsTest {

    /*
    CTOR
     */

    @Test
    fun `ctor accepts positive width and height`() {
        val b = SceneBounds(0.0, 0.0, 100.0, 50.0)
        assertEquals(100.0, b.width)
        assertEquals(50.0, b.height)
    }

    @Test
    fun `ctor rejects zero or negative width`() {
        assertFailsWith<IllegalArgumentException> { SceneBounds(0.0, 0.0, 0.0, 50.0) }
        assertFailsWith<IllegalArgumentException> { SceneBounds(0.0, 0.0, -1.0, 50.0) }
    }

    @Test
    fun `ctor rejects zero or negative height`() {
        assertFailsWith<IllegalArgumentException> { SceneBounds(0.0, 0.0, 100.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { SceneBounds(0.0, 0.0, 100.0, -1.0) }
    }

    /*
    FROM POLYGON
     */

    @Test
    fun `Polygon factory takes the vertex bounding box, no axis flip`() {
        val arena = Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))
        assertEquals(SceneBounds(-400.0, -300.0, 800.0, 600.0), SceneBounds(arena))
    }

    @Test
    fun `Polygon factory covers a non-rectangular vertex loop`() {
        val triangle = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(1.0, 3.0))
        assertEquals(SceneBounds(0.0, 0.0, 4.0, 3.0), SceneBounds(triangle))
    }

    /*
    WORLD BORDER
     */

    @Test
    fun `worldBorder circumscribes the centered rectangle`() {
        // 60×80 rectangle centered at CGA origin: half-diagonal = sqrt(30² + 40²) = 50.
        val border = worldBorder(60.0, 80.0)
        assertEquals(50.0, border.radius)
        assertEquals(FlatPoint(0.0, 0.0), border.center.toFlat)
        // All four rectangle corners lie exactly on the circle.
        assertTrue(FlatPoint(30.0, 40.0) on border)
        assertTrue(FlatPoint(-30.0, 40.0) on border)
        assertTrue(FlatPoint(30.0, -40.0) on border)
        assertTrue(FlatPoint(-30.0, -40.0) on border)
    }

    @Test
    fun `SceneBounds extension delegates to width and height, ignoring minX and minY`() {
        // The border is always centered at CGA origin — offset bounds don't move it.
        val offset = SceneBounds(15.0, -7.0, 60.0, 80.0).worldBorder()
        assertEquals(worldBorder(60.0, 80.0), offset)
        assertEquals(sqrt(30.0 * 30.0 + 40.0 * 40.0), offset.radius)
    }
}
