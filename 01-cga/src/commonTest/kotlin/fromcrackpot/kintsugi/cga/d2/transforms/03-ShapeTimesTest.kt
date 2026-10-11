package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ShapeTimesTest {

    private val motor: Versor = T(3.0, -2.0) * R(FlatPoint(1.0, 1.0), 30.deg)
    private val flector: Versor = F(Line(1.0, 2.0, 3.0))

    private val dipole = RoundPoint(0.0, 0.0) join RoundPoint(4.0, 2.0)
    private val circle = dipole join RoundPoint(1.0, 5.0)

    @Test
    fun `a Shape-typed primitive dispatches to its own action`() {
        val shapes: List<Shape> = listOf(FlatPoint(2.0, 3.0), RoundPoint(2.0, 3.0), Line(1.0, 2.0, 3.0), circle, dipole)
        assertEquals(FlatPoint(2.0, 3.0) * motor, shapes[0] * motor)
        assertEquals(RoundPoint(2.0, 3.0) * motor, shapes[1] * motor)
        assertEquals(Line(1.0, 2.0, 3.0) * motor, shapes[2] * motor)
        assertEquals(circle * motor, shapes[3] * motor)
        assertEquals(dipole * motor, shapes[4] * motor)
    }

    @Test
    fun `a Shape-typed composite dispatches to its own action`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(4.0, 2.0))
        val polygon = Polygon.Rectangle(FlatPoint(-1.0, -1.0), FlatPoint(2.0, 3.0))
        val polyline = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0), FlatPoint(3.0, -1.0)))
        val shapes: List<Shape> = listOf(segment, polygon, polyline)
        assertEquals(segment * motor, shapes[0] * motor)
        assertEquals(polygon * motor, shapes[1] * motor)
        assertEquals(polyline * motor, shapes[2] * motor)
    }

    @Test
    fun `an improper versor dispatches like a proper one`() {
        val polygon = Polygon.Rectangle(FlatPoint(-1.0, -1.0), FlatPoint(2.0, 3.0))
        assertEquals(FlatPoint(2.0, 3.0) * flector, (FlatPoint(2.0, 3.0) as Shape) * flector)
        assertEquals(polygon * flector, (polygon as Shape) * flector)
    }

    @Test
    fun `a refinement comes back as its own class through the dispatch`() {
        val shape: Shape = Direction(3.0, 4.0)
        val turned = shape * R(FlatPoint(0.0, 0.0), 90.deg)
        assertIs<Direction>(turned, "the copy hook preserves the receiver's class through the dispatch")
        assertEquals(Direction(-4.0, 3.0), turned)
    }
}
