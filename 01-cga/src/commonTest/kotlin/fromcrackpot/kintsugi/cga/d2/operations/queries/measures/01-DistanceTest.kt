package fromcrackpot.kintsugi.cga.d2.operations.queries.measures

import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DistanceTest {

    /*
    FLAT POINT
     */

    @Test
    fun `flat point to flat point measures the separation`() {
        assertEquals(5.0, FlatPoint(0.0, 0.0) distanceTo FlatPoint(3.0, 4.0))
    }

    @Test
    fun `flat point to itself measures zero`() {
        val p = FlatPoint(7.0, -2.0)
        assertEquals(0.0, p distanceTo p)
    }

    @Test
    fun `flat point distance is symmetric`() {
        val a = FlatPoint(1.5, -3.25)
        val b = FlatPoint(-4.0, 6.5)
        assertEquals(a distanceTo b, b distanceTo a)
    }

    @Test
    fun `flat point distance survives large coordinates`() {
        // Difference-first evaluation: the error scales with the separation, not the magnitude.
        val a = FlatPoint(1e6, 1e6)
        val b = FlatPoint(1e6 + 3.0, 1e6 + 4.0)
        assertEquals(5.0, a distanceTo b)
    }

    @Test
    fun `flat point to round point measures to its position`() {
        assertEquals(5.0, FlatPoint(1.0, 2.0) distanceTo RoundPoint(4.0, 6.0))
    }

    @Test
    fun `flat point to line measures the perpendicular drop`() {
        val line = Line(1.0, 0.0, -2.0) // x = 2
        assertEquals(3.0, FlatPoint(5.0, 1.0) distanceTo line)
    }

    @Test
    fun `flat point to line measures equally from both sides`() {
        val line = Line(1.0, 0.0, -2.0) // x = 2
        assertEquals(
            FlatPoint(5.0, 1.0) distanceTo line,
            FlatPoint(-1.0, 7.0) distanceTo line
        )
    }

    @Test
    fun `flat point on the line measures zero`() {
        assertEquals(0.0, FlatPoint(2.0, -9.0) distanceTo Line(1.0, 0.0, -2.0))
    }

    @Test
    fun `flat point to line equals the magnitude of the dot`() {
        val p = FlatPoint(-3.5, 2.25)
        val g = Line(3.0, 4.0, -1.0)
        assertEquals(abs(p dot g), p distanceTo g)
    }

    /*
    ROUND POINT
     */

    @Test
    fun `round point to round point measures the separation`() {
        assertEquals(5.0, RoundPoint(0.0, 0.0) distanceTo RoundPoint(3.0, 4.0))
    }

    @Test
    fun `round point distance is symmetric`() {
        val a = RoundPoint(1.5, -3.25)
        val b = RoundPoint(-4.0, 6.5)
        assertEquals(a distanceTo b, b distanceTo a)
    }

    @Test
    fun `a sized round point measures from its position`() {
        // Semantic aw (an imaginary size): the conformal component takes no part.
        val sized = RoundPoint(1.0, 2.0, 1.0, 0.0)
        assertEquals(5.0, sized distanceTo RoundPoint(4.0, 6.0))
    }

    @Test
    fun `round point to flat point measures to its position`() {
        assertEquals(5.0, RoundPoint(1.0, 2.0) distanceTo FlatPoint(4.0, 6.0))
    }

    @Test
    fun `mixed forms agree with the flat form`() {
        val r = RoundPoint(1.0, 2.0)
        val f = FlatPoint(4.0, 6.0)
        assertEquals(FlatPoint(1.0, 2.0) distanceTo f, r distanceTo f)
        assertEquals(FlatPoint(1.0, 2.0) distanceTo f, f distanceTo r)
    }

    @Test
    fun `round point to line measures the perpendicular drop`() {
        val line = Line(0.0, 1.0, -3.0) // y = 3
        assertEquals(4.0, RoundPoint(10.0, 7.0) distanceTo line)
    }

    @Test
    fun `round point on the line measures zero`() {
        assertEquals(0.0, RoundPoint(-8.0, 3.0) distanceTo Line(0.0, 1.0, -3.0))
    }

    @Test
    fun `round point to line equals the magnitude of the dot`() {
        val p = RoundPoint(-3.5, 2.25)
        val g = Line(3.0, 4.0, -1.0)
        assertEquals(abs(p dot g), p distanceTo g)
    }

    /*
    AT INFINITY
     */

    @Test
    fun `a point at infinity has no distance to a point`() {
        val direction = Direction(3.0, 4.0)
        val point = FlatPoint(1.0, 2.0)
        assertFailsWith<AtInfinityException> { direction distanceTo point }
        assertFailsWith<AtInfinityException> { point distanceTo direction }
        assertFailsWith<AtInfinityException> { direction.toRound distanceTo point.toRound }
        assertFailsWith<AtInfinityException> { point.toRound distanceTo direction.toRound }
        assertFailsWith<AtInfinityException> { point distanceTo direction.toRound }
        assertFailsWith<AtInfinityException> { direction.toRound distanceTo point }
    }

    @Test
    fun `an operand at infinity has no distance to a line`() {
        val line = Line(1.0, 0.0, -2.0)
        val horizon = Line(0.0, 0.0, 1.0)
        val point = FlatPoint(5.0, 1.0)
        assertFailsWith<AtInfinityException> { Direction(3.0, 4.0) distanceTo line }
        assertFailsWith<AtInfinityException> { point distanceTo horizon }
        assertFailsWith<AtInfinityException> { Direction(3.0, 4.0).toRound distanceTo line }
        assertFailsWith<AtInfinityException> { point.toRound distanceTo horizon }
    }
}
