package fromcrackpot.kintsugi.compose.drawing.primitives

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawRoundPoint` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn
 * contract — the expected trace is empty, never a throw.
 */
class RoundPointDrawTest {

    /* DRAW */

    @Test
    fun `round point renders as filled circle at CGA-to-canvas position with size as diameter`() {
        // CGA (20, −10) → canvas (120, 110). Size 8 → radius 4.
        val shape = RoundPointNode(
            RoundPoint(20.0, -10.0),
            ShapeStyle(fill = ShapeColor.rgb(0, 0xff, 0), size = 8.0),
        )
        val expected = "circle  c=(120, 110) r=4  fill=#00ff00"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `round point at infinity emits no draw calls`() {
        val shape = RoundPointNode(RoundPoint(1.0, 0.0, az = 0.0, aw = 1.0), ShapeStyle.POINT)
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }
}
