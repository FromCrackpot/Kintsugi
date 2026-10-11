package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.constraints.coerceOut
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PolygonNodeTest {

    private val style = ShapeStyle.POLYGON.copy(fill = ShapeColor.GREEN)
    private val box = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(10.0, 10.0))
    private val wall = -box

    /* COERCE OUT — WALL */

    @Test
    fun `coerceOut of a wall nudges the geometry and carries the style untouched`() {
        val subject = PolygonNode(Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)), style)
        val coerced = subject coerceOut wall
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(4.0, 6.0)).vertices, coerced.vertices)
        assertSame(style, coerced.style)
    }

    @Test
    fun `coerceOut accepts a leaf collider — a leaf is a polygon`() {
        val subject = PolygonNode(Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)), style)
        val collider = PolygonNode(wall)
        val coerced = subject coerceOut collider
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(4.0, 6.0)).vertices, coerced.vertices)
    }

    @Test
    fun `coerceOut returns this untouched when already inside the allowed region`() {
        val subject = PolygonNode(Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)), style)
        assertSame(subject, subject coerceOut wall)
    }

    /* COERCE OUT — OBSTACLE */

    @Test
    fun `coerceOut of an obstacle nudges the geometry and carries the style untouched`() {
        val subject = PolygonNode(Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0)), style)
        val coerced = subject coerceOut box
        assertEquals(Polygon.Rectangle(FlatPoint(4.0, 10.0), FlatPoint(6.0, 13.0)).vertices, coerced.vertices)
        assertSame(style, coerced.style)
    }

    @Test
    fun `coerceOut returns this untouched when clear of the obstacle`() {
        val subject = PolygonNode(Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0)), style)
        assertSame(subject, subject coerceOut box)
    }

}
