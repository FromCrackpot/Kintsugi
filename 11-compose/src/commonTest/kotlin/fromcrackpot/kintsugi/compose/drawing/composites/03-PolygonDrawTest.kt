package fromcrackpot.kintsugi.compose.drawing.composites

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawPolygon` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The one degenerate scene is the inside-out skip — the
 * Polygon ctor makes every other degenerate ring unrepresentable.
 */
class PolygonDrawTest {

    /* DRAW */

    @Test
    fun `fill-only rectangle polygon emits fill plus transparent stroke drawPath calls`() {
        // Model corners (−40, −20)..(40, 20) → model top-left (−40, 20) → canvas (60, 80).
        // Rectangles are Polygon quads now — the trace records path bounds, not a rect event.
        val shape = PolygonNode(
            Polygon.Rectangle(FlatPoint(-40.0, -20.0), FlatPoint(40.0, 20.0)),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0)),
        )
        val expected = """
            path    tl=(60, 80) size=(80, 40)  fill=#ff0000
            path    tl=(60, 80) size=(80, 40)  stroke=#00000000 w=0
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `filled and stroked rectangle polygon emits two drawPath calls - fill then stroke`() {
        // Same Fill-XOR-Stroke gotcha as the circle: Compose's DrawStyle can't carry both, so a
        // filled-and-stroked polygon needs two draw calls, fill first, stroke on top.
        val shape = PolygonNode(
            Polygon.Rectangle(FlatPoint(10.0, 20.0), FlatPoint(40.0, 70.0)),
            ShapeStyle(
                fill = ShapeColor.rgb(0xaa, 0xbb, 0xcc),
                stroke = ShapeColor.rgb(0x11, 0x22, 0x33),
                strokeWidth = 4.0,
            ),
        )
        // Corners (10, 20)..(40, 70) → model top-left (10, 70) → canvas (110, 30).
        val expected = """
            path    tl=(110, 30) size=(30, 50)  fill=#aabbcc
            path    tl=(110, 30) size=(30, 50)  stroke=#112233 w=4
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `filled and stroked polygon emits two drawPath calls - fill then stroke`() {
        // Triangle (0,0)/(40,0)/(0,30) → canvas (100,100)/(140,100)/(100,70); the trace records
        // the path's control-point bounds: top-left (100, 70), size 40×30. Same Fill-XOR-Stroke
        // two-call rule as circle.
        val shape = PolygonNode(
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(40.0, 0.0), FlatPoint(0.0, 30.0))),
            ShapeStyle(
                fill = ShapeColor.rgb(0xff, 0, 0),
                stroke = ShapeColor.rgb(0, 0, 0xff),
                strokeWidth = 2.0,
            ),
        )
        val expected = """
            path    tl=(100, 70) size=(40, 30)  fill=#ff0000
            path    tl=(100, 70) size=(40, 30)  stroke=#0000ff w=2
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `inside-out polygon emits no draw calls`() {
        // signedArea < 0 ⇒ inside-out orientation (solid region is everything outside the loop).
        // The loop itself is drawable geometry, but the fill it declares isn't — skip + warn.
        val shape = PolygonNode(
            -Polygon.Rectangle(FlatPoint(-40.0, -20.0), FlatPoint(40.0, 20.0)),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0)),
        )
        assertEquals("", canvasToTrace(listOf(shape), 200f, 200f))
    }
}
