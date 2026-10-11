package fromcrackpot.kintsugi.compose.drawing.primitives

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.products.meet
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawDipole` through the production `drawShape` dispatch — recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn
 * contract — the expected trace is empty, never a throw.
 */
class DipoleDrawTest {

    /* DRAW */

    @Test
    fun `dipole renders two filled dots at start end plus a dashed connector`() {
        // RoundPoint(-40, 0) ∧ RoundPoint(40, 0) → dipole on x-axis. After Dipole's unitization +
        // start/end derivation, contained points come back as (-40, 0) and (40, 0). On a 200×200
        // canvas with CGA origin at (100, 100): start → (60, 100), end → (140, 100).
        // Default DIPOLE style: fill = BLACK, size = 8 → dot radius 4, connector strokeWidth = 2 (size/4).
        val shape = DipoleNode(RoundPoint(-40.0, 0.0) join RoundPoint(40.0, 0.0))
        val expected = """
            circle  c=(60, 100) r=4  fill=#000000
            circle  c=(140, 100) r=4  fill=#000000
            line    p1=(60, 100) p2=(140, 100)  stroke=#000000 w=2 dash=on
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `dipole with fill-NONE still emits transparent draw calls`() {
        // Pins the uniform paint policy: every paint layer draws unconditionally, transparent
        // included — alpha-0 culling is Skia's job, not ours. A NONE-fill dipole therefore
        // produces three transparent draws (two dots + dashed connector).
        val shape = DipoleNode(
            RoundPoint(-40.0, 0.0) join RoundPoint(40.0, 0.0),
            ShapeStyle.NONE,
        )
        val expected = """
            circle  c=(60, 100) r=0  fill=#00000000
            circle  c=(140, 100) r=0  fill=#00000000
            line    p1=(60, 100) p2=(140, 100)  stroke=#00000000 w=0 dash=on
        """.trimIndent()
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `imaginary dipole emits no draw calls`() {
        // Two unit circles 10 apart don't intersect — Circle ∨ Circle returns an imaginary Dipole
        // (radius² < 0) carrying "they're 8 units apart in separation" info. No real points to
        // draw; drawDipole skip+warns, mirroring imaginary Circle.
        val dipole = Circle(0.0, 0.0, 1.0) meet Circle(10.0, 0.0, 1.0)
        val shape = DipoleNode(dipole, ShapeStyle.DIPOLE)
        assertEquals("", canvasToTrace(listOf(shape), 200f, 200f))
    }
}
