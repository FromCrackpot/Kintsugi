package fromcrackpot.kintsugi.compose.drawing.primitives

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawCircle` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn
 * contract — the expected trace is empty, never a throw.
 */
class CircleDrawTest {

    /* DRAW */

    @Test
    fun `fill-only circle emits fill plus transparent stroke draw calls`() {
        val shape = CircleNode(
            Circle(0.0, 0.0, 40.0),
            ShapeStyle(fill = ShapeColor.rgb(0, 0, 0xff)),
        )
        val expected = """
            circle  c=(100, 100) r=40  fill=#0000ff
            circle  c=(100, 100) r=40  stroke=#00000000 w=0
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `stroke-only circle emits transparent fill plus stroke draw calls`() {
        val shape = CircleNode(
            Circle(0.0, 0.0, 50.0),
            ShapeStyle(stroke = ShapeColor.rgb(0xff, 0xff, 0), strokeWidth = 3.0),
        )
        val expected = """
            circle  c=(100, 100) r=50  fill=#00000000
            circle  c=(100, 100) r=50  stroke=#ffff00 w=3
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `filled and stroked circle emits two drawCircle calls - fill then stroke`() {
        // Compose's DrawStyle is Fill XOR Stroke, so a circle with both needs two draw calls.
        // Order matters (fill first, stroke on top).
        val shape = CircleNode(
            Circle(0.0, 0.0, 60.0),
            ShapeStyle(
                fill = ShapeColor.rgb(0xaa, 0xbb, 0xcc),
                stroke = ShapeColor.rgb(0x11, 0x22, 0x33),
                strokeWidth = 4.0,
            ),
        )
        val expected = """
            circle  c=(100, 100) r=60  fill=#aabbcc
            circle  c=(100, 100) r=60  stroke=#112233 w=4
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `circle off-center maps via cgaToCanvas`() {
        // CGA (30, −20) → canvas (130, 120). Y is flipped.
        val shape = CircleNode(
            Circle(30.0, -20.0, 10.0),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0)),
        )
        val expected = """
            circle  c=(130, 120) r=10  fill=#ff0000
            circle  c=(130, 120) r=10  stroke=#00000000 w=0
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `imaginary circle emits no draw calls`() {
        // Raw 4-arg ctor: classic (cw = −1) circle at origin with cz = −2 ⇒
        //   radius² = cx² + cy² − 2·cz·cw = 0 + 0 − 2·(−2)·(−1) = −4 (negative ⇒ imaginary).
        // Friendly center/radius ctor can't produce this directly.
        val shape = CircleNode(
            Circle(cx = 0.0, cy = 0.0, cz = -2.0, cw = -1.0),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0)),
        )
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }

    @Test
    fun `inside-out circle emits no draw calls`() {
        // cw > 0 ⇒ inside-out orientation (interior is everywhere outside). Real radius² but skipped.
        // Circle(0, 0, −2, +1) ⇒ radius² = 0 + 0 − 2·(−2)·1 = 4, real; cw = +1, inside-out.
        val shape = CircleNode(
            Circle(cx = 0.0, cy = 0.0, cz = -2.0, cw = 1.0),
            ShapeStyle(fill = ShapeColor.rgb(0xff, 0, 0)),
        )
        assertEquals("", canvasToTrace(listOf(shape), 100f, 100f))
    }
}
