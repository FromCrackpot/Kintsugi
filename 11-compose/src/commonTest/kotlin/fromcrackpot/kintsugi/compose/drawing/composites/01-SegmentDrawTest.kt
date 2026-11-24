package fromcrackpot.kintsugi.compose.drawing.composites

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeDash
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Trace snapshots for `drawSegment` through the production `drawShape` dispatch: recorded draw
 * calls asserted against inline goldens. The DEGENERATE section pins the skip+warn contract:
 * the expected trace is empty, never a throw.
 */
class SegmentDrawTest {

    /* DRAW */

    @Test
    fun `segment renders as a single stroked line between start and end`() {
        // Segment(FlatPoint(-40, 0), FlatPoint(40, 0)) lifts to RoundPoint(-40, 0) ∧ RoundPoint(40, 0).
        // Containers: start → (-40, 0) → canvas (60, 100); end → (40, 0) → canvas (140, 100).
        // Default SEGMENT style: stroke = BLACK, strokeWidth = 1, no dots, no dash. That is the visual
        // distinction from a Dipole (which renders as two filled dots + dashed connector).
        val shape = SegmentNode(Segment(FlatPoint(-40.0, 0.0), FlatPoint(40.0, 0.0)))
        val expected = "line    p1=(60, 100) p2=(140, 100)  stroke=#000000 w=1"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `segment honors a non-butt strokeCap`() {
        // ShapeStyle.strokeCap threads through drawSegment into Compose's StrokeCap; the trace
        // annotates it with a `cap=round` suffix (Butt prints nothing, keeping other goldens stable).
        val shape = SegmentNode(
            Segment(FlatPoint(-40.0, 0.0), FlatPoint(40.0, 0.0)),
            ShapeStyle.SEGMENT.copy(strokeWidth = 8.0, strokeCap = ShapeStrokeCap.ROUND),
        )
        val expected = "line    p1=(60, 100) p2=(140, 100)  stroke=#000000 w=8 cap=round"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `authored dash strokes a real segment dashed`() {
        val shape = SegmentNode(
            Segment(FlatPoint(-40.0, 0.0), FlatPoint(40.0, 0.0)),
            ShapeStyle.SEGMENT.copy(dash = ShapeDash(listOf(3.0, 1.0), offset = 1.0)),
        )
        val expected = "line    p1=(60, 100) p2=(140, 100)  stroke=#000000 w=1 dash=on"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* IMAGINARY */

    @Test
    fun `imaginary segment draws dashed between its real-valued endpoints`() {
        // radius² = −1: the endpoints (−1, 1) and (−1, −1) are real-valued, no real chord joins them.
        val shape = SegmentNode(Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0).toSegment)
        val expected = "line    p1=(99, 99) p2=(99, 101)  stroke=#000000 w=1 dash=on"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `authored SOLID keeps an imaginary segment solid`() {
        val shape = SegmentNode(
            Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0).toSegment,
            ShapeStyle.SEGMENT.copy(dash = ShapeDash.SOLID),
        )
        val expected = "line    p1=(99, 99) p2=(99, 101)  stroke=#000000 w=1"
        assertEquals(expected, canvasToTrace(listOf(shape), 200f, 200f))
    }

    /* DEGENERATE */

    @Test
    fun `segment at infinity emits no draw calls`() {
        val shape = SegmentNode(Dipole(0.0, 0.0, 1.0, 0.0, 0.0, 0.0).toSegment)
        assertEquals("", canvasToTrace(listOf(shape), 200f, 200f))
    }

    @Test
    fun `zero-length segment emits no draw calls - even with a round cap`() {
        val shape = SegmentNode(
            Dipole(0.0, 1.0, 0.0, 0.0, 0.0, 0.0).toSegment,
            ShapeStyle.SEGMENT.copy(strokeWidth = 8.0, strokeCap = ShapeStrokeCap.ROUND),
        )
        assertEquals("", canvasToTrace(listOf(shape), 200f, 200f))
    }
}
