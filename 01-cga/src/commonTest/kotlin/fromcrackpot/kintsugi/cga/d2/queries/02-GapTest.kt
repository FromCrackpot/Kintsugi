package fromcrackpot.kintsugi.cga.d2.queries

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GapTest {

    private val box = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(10.0, 10.0))
    private val wall = -box

    // Off the box's top-right corner: the unique closest pair is corner (13, 14) to corner (10, 10).
    private val offCorner = Polygon.Rectangle(FlatPoint(13.0, 14.0), FlatPoint(20.0, 20.0))

    /*
    Polygon <gap> Polygon — classic other
    */

    @Test
    fun `stacked rectangles gap along the shared normal`() {
        val paddle = Polygon.Rectangle(FlatPoint(-60.0, -240.0), FlatPoint(60.0, -220.0))
        val ball = Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))
        val g = assertNotNull(ball gap paddle)
        assertEquals(212.0, g.length, 1e-12)
        assertEquals(Direction(0.0, -212.0), g.end - g.start)
    }

    @Test
    fun `an offset corner pairs vertex to vertex`() {
        assertEquals(Segment(FlatPoint(13.0, 14.0), FlatPoint(10.0, 10.0)), offCorner gap box)
    }

    @Test
    fun `parallel faces tie on the pair but agree on the displacement`() {
        val subject = Polygon.Rectangle(FlatPoint(2.0, 15.0), FlatPoint(8.0, 20.0))
        val g = assertNotNull(subject gap box)
        assertEquals(5.0, g.length, 1e-12)
        assertEquals(Direction(0.0, -5.0), g.end - g.start)
    }

    @Test
    fun `the segment starts on the subject and ends on the other`() {
        val g = assertNotNull(offCorner gap box)
        assertTrue(g.start on offCorner)
        assertTrue(g.end on box)
    }

    @Test
    fun `swapping the operands reverses the segment`() {
        val there = assertNotNull(offCorner gap box)
        val back = assertNotNull(box gap offCorner)
        assertEquals(-there, back)
        assertEquals(there.length, back.length, 1e-12)
    }

    @Test
    fun `the answer is a plain segment`() {
        assertEquals(Segment::class, (assertNotNull(offCorner gap box))::class)
    }

    @Test
    fun `overlapping regions have no gap`() {
        assertNull(Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0)) gap box)
    }

    @Test
    fun `touching is not a gap - a vertex on a face`() {
        assertNull(Polygon.Rectangle(FlatPoint(10.0, 4.0), FlatPoint(12.0, 6.0)) gap box)
    }

    @Test
    fun `touching is not a gap - a face on a vertex`() {
        // Face x + y = 20 passes through the box's corner (10, 10).
        val touching = Polygon(listOf(FlatPoint(13.0, 11.0), FlatPoint(11.0, 13.0), FlatPoint(9.0, 11.0), FlatPoint(11.0, 9.0)))
        assertNull(touching gap box)
    }

    @Test
    fun `a subject nested inside a classic other has no gap`() {
        assertNull(Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)) gap box)
    }

    @Test
    fun `a concave other pairs to its nearest wall`() {
        // A U-loop whose slot between x = 4 and x = 6 opens upward; the subject floats in the slot.
        val u = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(10.0, 10.0), FlatPoint(6.0, 10.0),
                FlatPoint(6.0, 4.0), FlatPoint(4.0, 4.0), FlatPoint(4.0, 10.0), FlatPoint(0.0, 10.0),
            )
        )
        val subject = Polygon.Rectangle(FlatPoint(4.5, 6.0), FlatPoint(5.25, 7.0))
        val g = assertNotNull(subject gap u)
        assertEquals(0.5, g.length, 1e-12)
        assertEquals(Direction(-0.5, 0.0), g.end - g.start)
    }

    /*
    Polygon <gap> Polygon — inside-out other
    */

    @Test
    fun `a subject within an inside-out other gaps to the loop from inside`() {
        val subject = Polygon.Rectangle(FlatPoint(2.0, 3.0), FlatPoint(4.0, 5.0))
        val g = assertNotNull(subject gap wall)
        assertEquals(2.0, g.length, 1e-12)
        assertEquals(Direction(-2.0, 0.0), g.end - g.start)
        assertTrue(g.end on wall)
    }

    @Test
    fun `a subject outside the loop of an inside-out other sits in its solid - no gap`() {
        assertNull(Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0)) gap wall)
    }

    @Test
    fun `two inside-out polygons never have a gap`() {
        val other = -Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0))
        assertNull(wall gap other)
    }
}
