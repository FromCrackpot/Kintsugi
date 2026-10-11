package fromcrackpot.kintsugi.cga.d2.constraints

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.queries.collide
import fromcrackpot.kintsugi.cga.d2.queries.gap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class CoerceTest {

    private val box = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(10.0, 10.0))
    private val wall = -box

    /*
    Polygon <coerceOut> Polygon — inside-out collider (wall)
    */

    @Test
    fun `subject already inside the allowed region comes back as the same instance`() {
        val subject = Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0))
        assertSame(subject, subject coerceOut wall)
    }

    @Test
    fun `default retreats the subject back inside along the correcting normal`() {
        val subject = Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0))
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(4.0, 6.0)), subject coerceOut wall)
    }

    @Test
    fun `corner overshoot converges through resolve-and-requery`() {
        var subject = Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0))
        while (true) subject = (subject coerceOut wall).takeIf { it != subject } ?: break
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(5.0, 3.0)), subject)
    }

    /*
    Polygon <coerceOut> Polygon — classic collider (obstacle)
    */

    @Test
    fun `disjoint subject comes back as the same instance`() {
        val subject = Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0))
        assertSame(subject, subject coerceOut box)
    }

    @Test
    fun `default nudges the subject out along the correcting normal`() {
        val subject = Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0))
        assertEquals(Polygon.Rectangle(FlatPoint(4.0, 10.0), FlatPoint(6.0, 13.0)), subject coerceOut box)
    }

    /*
    Polygon <coerceIn> Polygon — classic boundary
    */

    @Test
    fun `subject already inside the boundary comes back as the same instance`() {
        val subject = Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0))
        assertSame(subject, subject coerceIn box)
    }

    @Test
    fun `default nudges the subject back inside along the correcting normal`() {
        val subject = Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0))
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(4.0, 6.0)), subject coerceIn box)
    }

    @Test
    fun `corner overshoot converges back inside through resolve-and-requery`() {
        var subject = Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0))
        while (true) subject = (subject coerceIn box).takeIf { it != subject } ?: break
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(5.0, 3.0)), subject)
    }

    /*
    Polygon <coerceIn> Polygon — inside-out boundary
    */

    @Test
    fun `inside-out boundary keeps the subject in the complement`() {
        val subject = Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0))
        assertEquals(Polygon.Rectangle(FlatPoint(4.0, 10.0), FlatPoint(6.0, 13.0)), subject coerceIn wall)
    }

    /*
    Duality — coerceIn b ≡ coerceOut -b, coerceOut b ≡ coerceIn -b
    */

    @Test
    fun `coerceIn of a boundary lands where coerceOut of its negation does`() {
        val subject = Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0))
        assertEquals(subject coerceOut wall, subject coerceIn box)
    }

    @Test
    fun `coerceOut of an obstacle lands where coerceIn of its negation does`() {
        val subject = Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0))
        assertEquals(subject coerceIn wall, subject coerceOut box)
    }

    /*
    Polygon <coerceOn> Polygon — classic support
    */

    @Test
    fun `a separated subject is drawn in until it touches`() {
        val paddle = Polygon.Rectangle(FlatPoint(-60.0, -240.0), FlatPoint(60.0, -220.0))
        val ball = Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))
        val landed = ball coerceOn paddle
        assertEquals(FlatPoint(0.0, -212.0), landed.centroid)
        assertNull(landed collide paddle)
        assertNull(landed gap paddle)
    }

    @Test
    fun `an overlapping subject lands where coerceOut lands`() {
        val subject = Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0))
        assertEquals(Polygon.Rectangle(FlatPoint(4.0, 10.0), FlatPoint(6.0, 13.0)), subject coerceOn box)
        assertEquals(subject coerceOut box, subject coerceOn box)
    }

    @Test
    fun `a touching subject comes back as the same instance`() {
        val subject = Polygon.Rectangle(FlatPoint(10.0, 4.0), FlatPoint(12.0, 6.0))
        assertSame(subject, subject coerceOn box)
    }

    @Test
    fun `an offset corner rests corner on corner`() {
        val subject = Polygon.Rectangle(FlatPoint(13.0, 14.0), FlatPoint(20.0, 20.0))
        assertEquals(Polygon.Rectangle(FlatPoint(10.0, 10.0), FlatPoint(17.0, 16.0)), subject coerceOn box)
    }

    @Test
    fun `resting is idempotent`() {
        val once = Polygon.Rectangle(FlatPoint(13.0, 14.0), FlatPoint(20.0, 20.0)) coerceOn box
        assertSame(once, once coerceOn box)
    }

    @Test
    fun `the subject's class is preserved through its copy hook`() {
        class Tri(polygon: Polygon) : Polygon(polygon.vertices) {
            override fun copy(polygon: Polygon): Polygon = Tri(polygon)
        }

        val tri = Tri(Polygon(listOf(FlatPoint(20.0, 20.0), FlatPoint(24.0, 20.0), FlatPoint(20.0, 23.0))))
        assertIs<Tri>(tri coerceOn box)
    }

    /*
    Polygon <coerceOn> Polygon — inside-out support
    */

    @Test
    fun `a subject within an inside-out support rests against the inner wall`() {
        val subject = Polygon.Rectangle(FlatPoint(2.0, 3.0), FlatPoint(4.0, 5.0))
        val landed = subject coerceOn wall
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 3.0), FlatPoint(2.0, 5.0)), landed)
        assertNull(landed gap wall)
    }

    @Test
    fun `a subject protruding from an inside-out support is retreated back to touching`() {
        val subject = Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0))
        assertEquals(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(4.0, 6.0)), subject coerceOn wall)
    }
}
