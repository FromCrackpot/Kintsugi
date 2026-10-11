package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class IntersectionTest {

    private val quad = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))

    /*
    Segment <intersects> Line
    */

    @Test
    fun `segment crossing a line intersects it`() {
        assertTrue(Segment(FlatPoint(0.0, -1.0), FlatPoint(0.0, 1.0)) intersects Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `segment touching a line at an endpoint intersects it (closed)`() {
        assertTrue(Segment(FlatPoint(0.0, 0.0), FlatPoint(0.0, 2.0)) intersects Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `segment strictly on one side of a line does not intersect it`() {
        assertFalse(Segment(FlatPoint(0.0, 1.0), FlatPoint(0.0, 2.0)) intersects Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `segment collinear with a line intersects it`() {
        assertTrue(Segment(FlatPoint(1.0, 0.0), FlatPoint(3.0, 0.0)) intersects Line(0.0, 1.0, 0.0))
    }

    /*
    Segment <intersects> Segment
    */

    @Test
    fun `X crossing intersects (interior-interior, no endpoint contact)`() {
        // The case the `on` probes cannot see — only the mutual straddle catches it.
        val h = Segment(FlatPoint(-1.0, 0.0), FlatPoint(1.0, 0.0))
        val v = Segment(FlatPoint(0.0, -1.0), FlatPoint(0.0, 1.0))
        assertTrue(h intersects v)
        assertTrue(v intersects h)
    }

    @Test
    fun `T-contact intersects (endpoint on interior)`() {
        val bar = Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0))
        val stem = Segment(FlatPoint(1.0, 0.0), FlatPoint(1.0, 1.0))
        assertTrue(bar intersects stem)
        assertTrue(stem intersects bar)
    }

    @Test
    fun `endpoint-to-endpoint touch intersects (closed)`() {
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))
        val b = Segment(FlatPoint(1.0, 0.0), FlatPoint(2.0, 1.0))
        assertTrue(a intersects b)
    }

    @Test
    fun `one-sided carrier straddle is not an intersection (mutual gate)`() {
        // b straddles a's carrier (y = 0) but a sits entirely left of b's carrier (x = 5).
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))
        val b = Segment(FlatPoint(5.0, -1.0), FlatPoint(5.0, 1.0))
        assertFalse(a intersects b)
        assertFalse(b intersects a)
    }

    @Test
    fun `parallel non-collinear segments do not intersect`() {
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0))
        val b = Segment(FlatPoint(0.0, 1.0), FlatPoint(2.0, 1.0))
        assertFalse(a intersects b)
    }

    @Test
    fun `collinear partial overlap intersects`() {
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0))
        val b = Segment(FlatPoint(1.0, 0.0), FlatPoint(3.0, 0.0))
        assertTrue(a intersects b)
        assertTrue(b intersects a)
    }

    @Test
    fun `collinear nested segments intersect (both nesting directions)`() {
        // inner ⊂ outer pins the third `on` probe (neither of outer's endpoints is on inner).
        val inner = Segment(FlatPoint(1.0, 0.0), FlatPoint(2.0, 0.0))
        val outer = Segment(FlatPoint(0.0, 0.0), FlatPoint(3.0, 0.0))
        assertTrue(inner intersects outer)
        assertTrue(outer intersects inner)
    }

    @Test
    fun `collinear segments touching at endpoints intersect (closed)`() {
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))
        val b = Segment(FlatPoint(1.0, 0.0), FlatPoint(2.0, 0.0))
        assertTrue(a intersects b)
    }

    @Test
    fun `collinear disjoint segments do not intersect (collinear gate)`() {
        // Without the collinear branch, all four dots round to 0 and the mutual
        // straddle (0 · 0 <= 0 both ways) would wrongly say true.
        val a = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))
        val b = Segment(FlatPoint(2.0, 0.0), FlatPoint(3.0, 0.0))
        assertFalse(a intersects b)
        assertFalse(b intersects a)
    }

    /*
    Polygon <intersects> Line (and the flipped delegation)
    */

    @Test
    fun `line through the interior intersects the polygon`() {
        assertTrue(quad intersects Line(0.0, 1.0, -3.0))    // y = 3 slices the quad
        assertTrue(Line(0.0, 1.0, -3.0) intersects quad)    // flipped delegation
    }

    @Test
    fun `line through a single vertex intersects the polygon (closed)`() {
        assertTrue(quad intersects Line(1.0, 1.0, -3.0))    // x + y = 3 touches only (1, 2)
    }

    @Test
    fun `line missing the polygon does not intersect it`() {
        assertFalse(quad intersects Line(0.0, 1.0, 0.0))    // y = 0, below the quad
        assertFalse(Line(0.0, 1.0, 0.0) intersects quad)
    }

    @Test
    fun `line passing through a concave notch still intersects (crosses edges to get there)`() {
        // Same CCW L-shape as the winding tests; x = 3 runs through the notch AND the
        // lateral arm — an infinite line overlapping the region must cross the boundary.
        val ell = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 2.0),
                FlatPoint(2.0, 2.0), FlatPoint(2.0, 4.0), FlatPoint(0.0, 4.0),
            )
        )
        assertTrue(ell intersects Line(1.0, 0.0, -3.0))     // x = 3
    }

    /*
    Segment <intersects> Polygon (and the flipped delegation)
    */

    @Test
    fun `segment crossing a polygon edge intersects it`() {
        val s = Segment(FlatPoint(0.0, 3.0), FlatPoint(2.0, 3.0))   // crosses the left edge x = 1
        assertTrue(s intersects quad)
        assertTrue(quad intersects s)                               // flipped delegation
    }

    @Test
    fun `segment entirely interior intersects the polygon (inside fallback)`() {
        // No edge contact at all — pins the `start inside` probe.
        assertTrue(Segment(FlatPoint(2.0, 3.0), FlatPoint(4.0, 4.0)) intersects quad)
    }

    @Test
    fun `segment entirely exterior does not intersect the polygon`() {
        assertFalse(Segment(FlatPoint(6.0, 3.0), FlatPoint(7.0, 3.0)) intersects quad)
    }

    @Test
    fun `segment floating in a concave notch does not intersect the polygon`() {
        // Inside the L's bounding box but outside the region — region semantics, not hull.
        val ell = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 2.0),
                FlatPoint(2.0, 2.0), FlatPoint(2.0, 4.0), FlatPoint(0.0, 4.0),
            )
        )
        assertFalse(Segment(FlatPoint(2.5, 2.5), FlatPoint(3.5, 3.5)) intersects ell)
    }

    @Test
    fun `segment with an endpoint on the boundary intersects the polygon (closed)`() {
        assertTrue(Segment(FlatPoint(5.0, 3.0), FlatPoint(7.0, 3.0)) intersects quad)
    }

    /*
    Polygon <intersects> Polygon
    */

    @Test
    fun `partially overlapping polygons intersect`() {
        val other = Polygon.Rectangle(FlatPoint(4.0, 4.0), FlatPoint(6.0, 6.0))
        assertTrue(quad intersects other)
        assertTrue(other intersects quad)
    }

    @Test
    fun `nested polygons intersect (both directions)`() {
        // small ⊂ quad: the per-edge interior probe fires one way, the trailing
        // `other.vertices.first() in this` probe the other way.
        val small = Polygon.Rectangle(FlatPoint(2.0, 3.0), FlatPoint(3.0, 4.0))
        assertTrue(small intersects quad)
        assertTrue(quad intersects small)
    }

    @Test
    fun `disjoint polygons do not intersect`() {
        val far = Polygon.Rectangle(FlatPoint(10.0, 10.0), FlatPoint(12.0, 12.0))
        assertFalse(quad intersects far)
        assertFalse(far intersects quad)
    }

    @Test
    fun `adjacent polygons sharing an edge intersect (closed)`() {
        // Two bricks side by side: shared boundary, zero-area overlap — touching counts.
        val neighbor = Polygon.Rectangle(FlatPoint(5.0, 2.0), FlatPoint(7.0, 5.0))
        assertTrue(quad intersects neighbor)
        assertTrue(neighbor intersects quad)
    }

    @Test
    fun `polygons touching vertex-to-vertex intersect (closed)`() {
        val corner = Polygon.Rectangle(FlatPoint(5.0, 5.0), FlatPoint(7.0, 7.0))
        assertTrue(quad intersects corner)
        assertTrue(corner intersects quad)
    }

    /*
    ORIENTATION (inside-out polygons)
    */

    @Test
    fun `any line intersects an inside-out polygon`() {
        val wall = -quad
        assertTrue(wall intersects Line(0.0, 1.0, 0.0))   // y = 0 misses the loop, reaches the far field
        assertTrue(Line(0.0, 1.0, 0.0) intersects wall)   // flipped delegation
    }

    @Test
    fun `segment in the allowed region does not intersect the wall`() {
        assertFalse(Segment(FlatPoint(2.0, 3.0), FlatPoint(4.0, 4.0)) intersects -quad)
    }

    @Test
    fun `segment in the far field intersects the wall's solid region`() {
        assertTrue(Segment(FlatPoint(6.0, 3.0), FlatPoint(7.0, 3.0)) intersects -quad)
    }

    @Test
    fun `segment touching the wall's boundary intersects (closed)`() {
        // Endpoint on the loop, body in the allowed region — on implies intersects either way.
        assertTrue(Segment(FlatPoint(2.0, 3.0), FlatPoint(1.0, 3.0)) intersects -quad)
    }

    @Test
    fun `two inside-out polygons always intersect (shared far field)`() {
        val a = -quad
        val b = -Polygon.Rectangle(FlatPoint(10.0, 10.0), FlatPoint(12.0, 12.0))
        assertTrue(a intersects b)
        assertTrue(b intersects a)
    }

    @Test
    fun `classic polygon in the wall's allowed region does not intersect it`() {
        val wall = -quad
        val small = Polygon.Rectangle(FlatPoint(2.0, 3.0), FlatPoint(3.0, 4.0))
        assertFalse(small intersects wall)
        assertFalse(wall intersects small)
    }

    @Test
    fun `classic polygon in the far field intersects the wall`() {
        val wall = -quad
        val far = Polygon.Rectangle(FlatPoint(10.0, 10.0), FlatPoint(12.0, 12.0))
        assertTrue(far intersects wall)
        assertTrue(wall intersects far)
    }
}
