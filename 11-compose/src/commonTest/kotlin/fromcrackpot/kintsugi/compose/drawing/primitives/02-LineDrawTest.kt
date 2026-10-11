package fromcrackpot.kintsugi.compose.drawing.primitives

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawLine` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn
 * contract — the expected trace is empty, never a throw.
 */
class LineDrawTest {

    /* DRAW */

    @Test
    fun `line clips to worldBorder via meet and draws a stroked segment`() {
        // Horizontal line y = 0 (joining two distinct points on it) meets the 200×200 worldBorder
        // (r = sqrt(2)·100 ≈ 141.421) at (±141.421, 0). Maps to canvas y = 100 with x = 100 ± 141.421.
        // Endpoint order follows dipole.start → dipole.end from the meet operator.
        val shape = LineNode(
            FlatPoint(-50.0, 0.0) join FlatPoint(50.0, 0.0),
            ShapeStyle(stroke = ShapeColor.rgb(0x80, 0x80, 0x80), strokeWidth = 2.0),
        )
        val expected = "line    p1=(-41.421, 100) p2=(241.421, 100)  stroke=#808080 w=2"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `line outside viewport (imaginary meet with worldBorder) emits no draw calls`() {
        // Line x = 1000: gx·x + gy·y + gz = 0 ⇒ Line(1, 0, −1000). On a 100×100 canvas the
        // worldBorder has radius sqrt(2)·50 ≈ 70.71 — the line is 1000 away from origin, so the
        // `line meet worldBorder` Dipole is imaginary and `drawLine` returns without drawing.
        val shape = LineNode(Line(1.0, 0.0, -1000.0), ShapeStyle.LINE)
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }

    @Test
    fun `line at infinity emits no draw calls`() {
        val shape = LineNode(Line(0.0, 0.0, 1.0), ShapeStyle.LINE)
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }
}
