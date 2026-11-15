package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SegmentNodeTest {

    private fun Segment.components() = listOf(gx, gy, gz, px, py, pz)

    /* CTOR */

    @Test
    fun `leaf shares its segment's dipole and the endpoints' instances`() {
        val segment = Segment(FlatPoint(1.0, 2.0), FlatPoint(4.0, -3.0))
        val node = SegmentNode(segment)
        assertSame(segment.toDipole, node.toDipole)
        assertSame(segment.start, node.start)
        assertSame(segment.end, node.end)
    }

    @Test
    fun `leaf wraps segments at infinity - imaginary - and of zero length`() {
        val atInfinity = Dipole(0.0, 0.0, 1.0, 0.0, 0.0, 0.0).toSegment
        val imaginary = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0).toSegment
        val zeroLength = Dipole(0.0, 1.0, 0.0, 0.0, 0.0, 0.0).toSegment
        for (segment in listOf(atInfinity, imaginary, zeroLength)) {
            assertSame(segment.toDipole, SegmentNode(segment).toDipole)
        }
    }

    /* COPY */

    @Test
    fun `moved leaf matches the moved plain segment exactly at large coordinates`() {
        // Re-deriving the endpoints on the wrap drifted past the SCALE quantum here.
        val plain = Segment(FlatPoint(1e4, 3e3), FlatPoint(1e4 + 7.0, 3e3 + 3.0))
        val node = SegmentNode(plain)
        val translator = T(123.456, -78.9)
        val dilator = D(FlatPoint(5.0, 5.0), 1.7)
        assertEquals((plain * translator).components(), (node * translator).components())
        assertEquals((plain * dilator).components(), (node * dilator).components())
    }

    @Test
    fun `moved leaf survives coordinates where re-wedging the endpoints threw`() {
        val plain = Segment(FlatPoint(1e5, 3e4), FlatPoint(1e5 + 7.0, 3e4 + 3.0))
        val rotor = R(FlatPoint(0.0, 0.0), 37.deg)
        assertEquals((plain * rotor).components(), (SegmentNode(plain) * rotor).components())
    }
}
