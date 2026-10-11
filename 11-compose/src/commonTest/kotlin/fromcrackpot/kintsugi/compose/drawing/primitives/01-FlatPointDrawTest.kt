package fromcrackpot.kintsugi.compose.drawing.primitives

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawFlatPoint` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn
 * contract — the expected trace is empty, never a throw.
 */
class FlatPointDrawTest {

    /* DRAW */

    @Test
    fun `flat point renders as filled rect centered at CGA-to-canvas position`() {
        // CGA (50, 30) → canvas (100 + 50, 100 − 30) = (150, 70). Size 8 → top-left (146, 66).
        val shape = FlatPointNode(
            FlatPoint(50.0, 30.0),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0), size = 8.0),
        )
        val expected = "rect    tl=(146, 66) size=(8, 8)  fill=#ff0000"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `flat point at infinity emits no draw calls`() {
        val shape = FlatPointNode(FlatPoint(1.0, 0.0, pz = 0.0), ShapeStyle.POINT)
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }
}
