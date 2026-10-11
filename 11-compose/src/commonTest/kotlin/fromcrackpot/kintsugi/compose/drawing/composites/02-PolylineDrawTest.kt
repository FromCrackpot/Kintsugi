package fromcrackpot.kintsugi.compose.drawing.composites

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawPolyline` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. No degenerate scenes — the Polyline ctor makes
 * degenerate chains unrepresentable.
 */
class PolylineDrawTest {

    /* DRAW */

    @Test
    fun `stroke-only polyline emits transparent fill plus stroke drawPath calls over the open chain`() {
        // Open chain (0,0)→(40,0)→(0,30) → canvas (100,100)/(140,100)/(100,70); the trace records
        // the path's control-point bounds — same shape as the polygon golden, but the path has
        // no close() (the trace can't show that; the open-stroke cap threading is what differs).
        val shape = PolylineNode(
            Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(40.0, 0.0), FlatPoint(0.0, 30.0))),
            ShapeStyle(stroke = ShapeColor.rgb(0, 0, 0xff), strokeWidth = 2.0),
        )
        val expected = """
            path    tl=(100, 70) size=(40, 30)  fill=#00000000
            path    tl=(100, 70) size=(40, 30)  stroke=#0000ff w=2
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }
}
