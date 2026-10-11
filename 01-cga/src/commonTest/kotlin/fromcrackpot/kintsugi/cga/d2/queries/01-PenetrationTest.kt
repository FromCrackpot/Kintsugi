package fromcrackpot.kintsugi.cga.d2.queries

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class PenetrationTest {

    // CCW quad (0,0)-(10,10); edges[0]=bottom, [1]=right, [2]=top, [3]=left (factory-order invariant).
    private val box = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(10.0, 10.0))

    // The same loop inside out: solid outside, the box's interior the allowed region.
    // Reversal maps side i to edge (2 − i) mod 4 — left stays [3], top becomes [0].
    private val wall = -box

    // Straddles the box's top face: 1 into the box, 2 out of it.
    private val straddler = Polygon.Rectangle(FlatPoint(4.0, 9.0), FlatPoint(6.0, 12.0))

    // A diamond off the box's top-right corner, disjoint from it: no box face clears all its
    // vertices — only its own face x + y = 20.5 separates the two.
    private val diamond = Polygon(listOf(FlatPoint(12.5, 11.0), FlatPoint(11.0, 12.5), FlatPoint(9.5, 11.0), FlatPoint(11.0, 9.5)))

    /*
    Polygon <collide> Polygon — inside-out collider (wall)
    */

    @Test
    fun `subject fully inside the allowed region yields no penetration`() {
        assertNull(Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)) collide wall)
    }

    @Test
    fun `subject touching the wall yields no penetration (touching is not penetration)`() {
        assertNull(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(2.0, 6.0)) collide wall)
    }

    @Test
    fun `protrusion past the left face reports that face, its depth, and the retreat normal`() {
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)) collide wall)
        assertSame(wall.edges[3], penetration.edge)
        assertEquals(2.0, penetration.depth, 1e-12)
        assertEquals(Direction(1.0, 0.0), penetration.normal)
    }

    @Test
    fun `corner overshoot reports the deeper of the two violated faces`() {
        // 3 past the left face, 1 below the bottom face.
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0)) collide wall)
        assertSame(wall.edges[3], penetration.edge)
        assertEquals(3.0, penetration.depth, 1e-12)
        assertEquals(Direction(1.0, 0.0), penetration.normal)
    }

    /*
    Polygon <collide> Polygon — classic collider (obstacle)
    */

    @Test
    fun `disjoint subject yields no penetration`() {
        assertNull(Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0)) collide box)
    }

    @Test
    fun `subject touching the obstacle yields no penetration (touching is not penetration)`() {
        assertNull(Polygon.Rectangle(FlatPoint(10.0, 4.0), FlatPoint(12.0, 6.0)) collide box)
    }

    @Test
    fun `shallow overlap through the top reports the top face, its depth, and the outward normal`() {
        val penetration = assertNotNull(straddler collide box)
        assertSame(box.edges[2], penetration.edge)
        assertEquals(1.0, penetration.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), penetration.normal)
    }

    @Test
    fun `subject fully inside reports the least-penetration face`() {
        // Nearest way out is up: 3 to the top face vs 6/6/7 for left/right/bottom.
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(4.0, 7.0), FlatPoint(6.0, 9.0)) collide box)
        assertSame(box.edges[2], penetration.edge)
        assertEquals(3.0, penetration.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), penetration.normal)
    }

    @Test
    fun `orientation dispatches the strategy - the same overlap answers as obstacle or wall`() {
        // As an obstacle the box pushes the straddler up and out (shallowest push-out); as a wall
        // the same loop retreats it down and back inside (deepest protrusion).
        val out = assertNotNull(straddler collide box)
        assertEquals(1.0, out.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), out.normal)
        val back = assertNotNull(straddler collide wall)
        assertEquals(2.0, back.depth, 1e-12)
        assertEquals(Direction(0.0, -1.0), back.normal)
    }

    /*
    Polygon <escape> Polygon — classic boundary (container)
    */

    @Test
    fun `subject fully inside the boundary yields no penetration`() {
        assertNull(Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)) escape box)
    }

    @Test
    fun `subject touching the boundary from inside yields no penetration (touching is not penetration)`() {
        assertNull(Polygon.Rectangle(FlatPoint(0.0, 4.0), FlatPoint(2.0, 6.0)) escape box)
    }

    @Test
    fun `protrusion past the left face reports the boundary's own face, its depth, and the inward normal`() {
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)) escape box)
        assertSame(box.edges[3], penetration.edge)
        assertEquals(2.0, penetration.depth, 1e-12)
        assertEquals(Direction(1.0, 0.0), penetration.normal)
    }

    @Test
    fun `corner overshoot out of the boundary reports the deeper of the two violated faces`() {
        // 3 past the left face, 1 below the bottom face.
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0)) escape box)
        assertSame(box.edges[3], penetration.edge)
        assertEquals(3.0, penetration.depth, 1e-12)
        assertEquals(Direction(1.0, 0.0), penetration.normal)
    }

    /*
    Polygon <escape> Polygon — inside-out boundary (the loop's interior forbidden)
    */

    @Test
    fun `subject clear of the loop yields no penetration`() {
        assertNull(Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0)) escape wall)
    }

    @Test
    fun `subject touching the loop from outside yields no penetration (touching is not penetration)`() {
        assertNull(Polygon.Rectangle(FlatPoint(10.0, 4.0), FlatPoint(12.0, 6.0)) escape wall)
    }

    @Test
    fun `shallow reach into the loop through the top reports the top face, its depth, and the outward normal`() {
        val penetration = assertNotNull(straddler escape wall)
        assertSame(wall.edges[0], penetration.edge)
        assertEquals(1.0, penetration.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), penetration.normal)
    }

    @Test
    fun `subject fully inside the loop reports the least-push-out face`() {
        val penetration = assertNotNull(Polygon.Rectangle(FlatPoint(4.0, 7.0), FlatPoint(6.0, 9.0)) escape wall)
        assertSame(wall.edges[0], penetration.edge)
        assertEquals(3.0, penetration.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), penetration.normal)
    }

    /*
    Duality — escape b ≡ collide -b, collide b ≡ escape -b
    */

    private fun assertDual(a: Penetration?, b: Penetration?) {
        val x = assertNotNull(a)
        val y = assertNotNull(b)
        assertEquals(x.depth, y.depth, 1e-12)
        assertEquals(x.normal, y.normal)
        // The negated polygon's edge is the reversed segment — the same face as an endpoint pair.
        assertEquals(setOf(x.edge.start, x.edge.end), setOf(y.edge.start, y.edge.end))
    }

    @Test
    fun `escape of a boundary agrees with collide of its negation`() {
        for (subject in listOf(
            Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)),
            Polygon.Rectangle(FlatPoint(-3.0, -1.0), FlatPoint(2.0, 2.0)),
        )) assertDual(subject escape box, subject collide -box)
    }

    @Test
    fun `collide of an obstacle agrees with escape of its negation`() {
        for (subject in listOf(
            straddler,
            Polygon.Rectangle(FlatPoint(4.0, 7.0), FlatPoint(6.0, 9.0)),
        )) assertDual(subject collide box, subject escape -box)
    }

    @Test
    fun `the duality holds for the null answer too`() {
        val inside = Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0))
        assertNull(inside escape box)
        assertNull(inside collide -box)
        val clear = Polygon.Rectangle(FlatPoint(20.0, 20.0), FlatPoint(22.0, 22.0))
        assertNull(clear collide box)
        assertNull(clear escape -box)
    }

    /*
    ∃ gate — subject-face separation
    */

    @Test
    fun `a separation only the subject's own face witnesses yields no penetration`() {
        assertNull(diamond collide box)
        assertNull(diamond escape wall)
    }

    @Test
    fun `the same diamond shifted onto the corner does overlap`() {
        val overlapping = Polygon(listOf(FlatPoint(12.0, 10.5), FlatPoint(10.5, 12.0), FlatPoint(9.0, 10.5), FlatPoint(10.5, 9.0)))
        assertNotNull(overlapping collide box)
        assertNotNull(overlapping escape wall)
    }

    @Test
    fun `a subject face resting on a region vertex is touching, not penetration`() {
        // Face x + y = 20 passes through the box's corner (10, 10).
        val touching = Polygon(listOf(FlatPoint(13.0, 11.0), FlatPoint(11.0, 13.0), FlatPoint(9.0, 11.0), FlatPoint(11.0, 9.0)))
        assertNull(touching collide box)
        assertNull(touching escape wall)
    }

    @Test
    fun `a clockwise subject answers identically - only its vertices and faces matter, not its winding`() {
        assertNull(-diamond collide box)
        assertNull(-diamond escape wall)
        val out = assertNotNull(-straddler collide box)
        assertEquals(1.0, out.depth, 1e-12)
        assertEquals(Direction(0.0, 1.0), out.normal)
        val back = assertNotNull(-straddler escape box)
        assertEquals(2.0, back.depth, 1e-12)
        assertEquals(Direction(0.0, -1.0), back.normal)
    }

    /*
    Penetration? <invoke> onPenetration
    */

    @Test
    fun `handler runs on the penetration and the answer passes through`() {
        val answer = Polygon.Rectangle(FlatPoint(-2.0, 4.0), FlatPoint(2.0, 6.0)) collide wall
        var handled: Penetration? = null
        assertSame(answer, answer { handled = it })
        assertSame(answer, assertNotNull(handled))
    }

    @Test
    fun `null answer skips the handler and passes through`() {
        val answer = Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)) collide wall
        assertNull(answer { fail("onPenetration must not run without a penetration") })
    }

    @Test
    fun `elvis after the call is the no-penetration lane`() {
        var handled = false
        var missed = false
        (Polygon.Rectangle(FlatPoint(2.0, 2.0), FlatPoint(4.0, 4.0)) collide wall) { handled = true }
            ?: run { missed = true }
        assertFalse(handled)
        assertTrue(missed)
    }
}
