package fromcrackpot.kintsugi.cga.d2

import fromcrackpot.kintsugi.cga.d2.queries.measures.angleTo
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AxisTest {

    @Test
    fun `the axes carry their coordinate lines`() {
        assertTrue(FlatPoint(5.0, 0.0) on AXIS_X, "the X axis is y = 0")
        assertTrue(FlatPoint(0.0, 5.0) on AXIS_Y, "the Y axis is x = 0")
    }

    @Test
    fun `the axes run toward their positive coordinate`() {
        assertEquals(Direction(1.0, 0.0), AXIS_X.attitude)
        assertEquals(Direction(0.0, 1.0), AXIS_Y.attitude)
        assertEquals(0.rad, AXIS_X.attitude.angle)
        assertEquals(90.deg, AXIS_Y.attitude.angle)
    }

    @Test
    fun `the axis pair is a quarter turn counter-clockwise`() {
        assertEquals(90.deg, AXIS_X angleTo AXIS_Y)
    }
}
