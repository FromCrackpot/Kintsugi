package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentTimesTest {

    /* MOTOR */

    @Test
    fun `segment translates via its endpoints`() {
        val s = Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0)) * T(1.0, 3.0)
        assertEquals(RoundPoint(1.0, 3.0), s.start)
        assertEquals(RoundPoint(3.0, 3.0), s.end)
    }

    @Test
    fun `segment rotates via its endpoints`() {
        val s = Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0)) * R(FlatPoint(0.0, 0.0), 90.deg)
        assertEquals(RoundPoint(0.0, 0.0), s.start)
        assertEquals(RoundPoint(0.0, 2.0), s.end)
    }

    /* FLECTOR */

    @Test
    fun `segment reflects via its endpoints`() {
        val s = Segment(FlatPoint(1.0, 0.0), FlatPoint(3.0, 0.0)) * F(Line(0.0, 1.0, 0.0))
        assertEquals(RoundPoint(1.0, 0.0), s.start)
        assertEquals(RoundPoint(3.0, 0.0), s.end)
    }

    @Test
    fun `segment transflects via its endpoints`() {
        // Mirror = x-axis with attitude (1, 0): endpoints mirror in place, then glide by 2.
        val s = Segment(FlatPoint(1.0, 1.0), FlatPoint(3.0, 1.0)) * G(Line(0.0, -1.0, 0.0), 2.0)
        assertEquals(RoundPoint(3.0, -1.0), s.start)
        assertEquals(RoundPoint(5.0, -1.0), s.end)
    }
}
