package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ContainmentTest {

    private val quad = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))

    /*
    FlatPoint in Circle
    */

    @Test
    fun `flat point in classic circle returns true`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(FlatPoint(0.5, 0.0) in c)
        assertTrue(c contains FlatPoint(0.5, 0.0))
    }

    @Test
    fun `flat point on classic circle boundary is in (closed)`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(FlatPoint(1.0, 0.0) in c)   // boundary — closed, like 5 in 1..5
    }

    @Test
    fun `flat point beyond a classic circle is not in`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertFalse(FlatPoint(2.0, 0.0) in c)
        assertTrue(FlatPoint(2.0, 0.0) !in c)   // !in is the strict-outside spelling
    }

    @Test
    fun `sub-quantum boundary offset is both on and in (exact trichotomy at SCALE)`() {
        // The rounding tenet: in ⟺ round(dot) >= 0, on ⟺ round(dot) == 0, so a point
        // within one SCALE quantum of the boundary classifies as on AND in — no FP sliver.
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val nearBoundary = FlatPoint(1.0 + 1e-9, 0.0)
        assertTrue(nearBoundary on c)
        assertTrue(nearBoundary in c)
        assertFalse(nearBoundary !in c)   // on ⇒ in ⇒ not outside
    }

    /*
    RoundPoint in Circle
    */

    @Test
    fun `round point in classic circle returns true`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(RoundPoint(0.5, 0.0) in c)
        assertTrue(c contains RoundPoint(0.5, 0.0))
    }

    @Test
    fun `round point on classic circle boundary is in (closed)`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(RoundPoint(1.0, 0.0) in c)
    }

    @Test
    fun `round point beyond a classic circle is not in`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertFalse(RoundPoint(2.0, 0.0) in c)
        assertTrue(RoundPoint(2.0, 0.0) !in c)
    }

    /*
    Inside-out circle (orientation-aware)
    */

    @Test
    fun `point in bounded disk is not in an inside-out circle`() {
        // Same geometric unit circle at origin, oriented inside-out (cw = +1).
        // Inside-out's interior is the unbounded exterior, so (0.5, 0) is NOT in it —
        // the excluded disk is exactly where !in holds.
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0)
        assertFalse(FlatPoint(0.5, 0.0) in insideOut)
        assertTrue(FlatPoint(0.5, 0.0) !in insideOut)
        assertFalse(RoundPoint(0.5, 0.0) in insideOut)
    }

    @Test
    fun `point in unbounded exterior is in an inside-out circle`() {
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0)
        assertTrue(FlatPoint(2.0, 0.0) in insideOut)
        assertTrue(RoundPoint(2.0, 0.0) in insideOut)
    }

    @Test
    fun `boundary point is in an inside-out circle (closed)`() {
        // Closed semantics: the boundary joins the interior for BOTH orientations —
        // for an inside-out circle that means the boundary joins the unbounded exterior.
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0)
        assertTrue(FlatPoint(1.0, 0.0) in insideOut)
        assertTrue(RoundPoint(1.0, 0.0) in insideOut)
    }

    /*
    Circle in Circle (region containment)
    */

    @Test
    fun `smaller classic circle in bigger classic circle returns true`() {
        val outer = Circle(centerX = 0.0, centerY = 0.0, radius = 5.0)
        val inner = Circle(centerX = 1.0, centerY = 0.0, radius = 1.0)
        assertTrue(inner in outer)
        assertTrue(outer contains inner)
    }

    @Test
    fun `bigger classic circle is not in smaller classic circle`() {
        val outer = Circle(centerX = 0.0, centerY = 0.0, radius = 5.0)
        val inner = Circle(centerX = 1.0, centerY = 0.0, radius = 1.0)
        assertFalse(outer in inner)
    }

    @Test
    fun `internally tangent classic circles count as contained (closed)`() {
        // Inner circle of radius 1 centered at (4, 0); outer of radius 5 at origin → tangent at (5, 0).
        // The tangent point is ON the outer boundary — included under closed semantics.
        val outer = Circle(centerX = 0.0, centerY = 0.0, radius = 5.0)
        val inner = Circle(centerX = 4.0, centerY = 0.0, radius = 1.0)
        assertTrue(inner in outer)
        assertFalse(outer in inner)
    }

    @Test
    fun `disjoint classic circles are not in each other`() {
        val a = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val b = Circle(centerX = 5.0, centerY = 0.0, radius = 1.0)
        assertFalse(a in b)
        assertFalse(b in a)
    }

    @Test
    fun `identical classic circles are in each other (closed)`() {
        // Every point of a's boundary is ON b's boundary — closed containment holds.
        val a = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val b = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(a in b)
        assertTrue(b in a)
    }

    @Test
    fun `inside-out is never in a classic circle`() {
        // Geometrically: an unbounded interior cannot fit in a bounded one.
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0)
        val classic = Circle(centerX = 0.0, centerY = 0.0, radius = 5.0)
        assertFalse(insideOut in classic)
    }

    /*
    FlatPoint in Polygon (nonzero winding, closed)
    */

    @Test
    fun `interior point is in a convex polygon`() {
        assertTrue(FlatPoint(3.0, 4.0) in quad)
        assertTrue(quad contains FlatPoint(3.0, 4.0))
    }

    @Test
    fun `point beyond each edge is not in the quad`() {
        assertTrue(FlatPoint(3.0, 1.0) !in quad)   // below bottom
        assertTrue(FlatPoint(6.0, 4.0) !in quad)   // right of right
        assertTrue(FlatPoint(3.0, 6.0) !in quad)   // above top
        assertTrue(FlatPoint(0.0, 4.0) !in quad)   // left of left
    }

    @Test
    fun `polygon boundary is included (closed) - on implies in`() {
        assertTrue(FlatPoint(3.0, 2.0) in quad)   // on bottom edge
        assertTrue(FlatPoint(1.0, 2.0) in quad)   // vertex
        assertTrue(FlatPoint(5.0, 5.0) in quad)   // opposite vertex
    }

    @Test
    fun `an inside-out polygon contains the complement (winding is authority)`() {
        // Winding as signal: the CW loop is inside-out — its solid region is everything outside.
        val insideOut = Polygon(
            listOf(FlatPoint(1.0, 2.0), FlatPoint(1.0, 5.0), FlatPoint(5.0, 5.0), FlatPoint(5.0, 2.0))
        )
        assertTrue(insideOut.isInsideOut)
        assertFalse(FlatPoint(3.0, 4.0) in insideOut)   // within the loop — the excluded region
        assertTrue(FlatPoint(6.0, 4.0) in insideOut)    // beyond the loop — the solid region
    }

    @Test
    fun `notch point of a concave polygon is not in`() {
        // CCW L-shape: unit-4 square with the top-right 2×2 corner cut out.
        val ell = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 2.0),
                FlatPoint(2.0, 2.0), FlatPoint(2.0, 4.0), FlatPoint(0.0, 4.0),
            )
        )
        assertTrue(FlatPoint(1.0, 1.0) in ell)    // in the thick part
        assertTrue(FlatPoint(1.0, 3.0) in ell)    // in the upright arm
        assertTrue(FlatPoint(3.0, 1.0) in ell)    // in the lateral arm
        assertFalse(FlatPoint(3.0, 3.0) in ell)   // in the notch
    }

    @Test
    fun `pentagram center is in (nonzero winding rule)`() {
        // Self-intersecting 5-point star: pentagon vertices visited every second one.
        // The central pentagon has winding ±2 — nonzero says contained (SVG's default
        // fill-rule; even-odd would say outside). This is the rule pin — don't "fix" it.
        val r = 5.0
        val pentagon = List(5) { k ->
            val theta = PI / 2 + k * 2.0 * PI / 5
            FlatPoint(r * cos(theta), r * sin(theta))
        }
        val pentagram = Polygon(listOf(pentagon[0], pentagon[2], pentagon[4], pentagon[1], pentagon[3]))
        assertTrue(FlatPoint(0.0, 0.0) in pentagram)          // center, winding 2
        assertTrue(FlatPoint(0.0, 4.5) in pentagram)          // in the top arm, winding 1
        assertFalse(FlatPoint(4.0, 4.0) in pentagram)         // between arms, winding 0
        assertFalse(FlatPoint(6.0, 0.0) in pentagram)         // outside the hull
    }

    @Test
    fun `pinched polygon classifies by winding`() {
        // Self-touching figure-eight: two CCW lobes sharing the pinch vertex (2, 2) —
        // a non-adjacent repeated vertex, legal by design.
        val pinch = FlatPoint(2.0, 2.0)
        val eight = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), pinch,
                FlatPoint(4.0, 4.0), FlatPoint(0.0, 4.0), pinch,
            )
        )
        assertTrue(FlatPoint(2.0, 0.5) in eight)    // lower lobe
        assertTrue(FlatPoint(2.0, 3.5) in eight)    // upper lobe
        assertTrue(pinch in eight)                  // the pinch point is on the boundary
        assertFalse(FlatPoint(5.0, 2.0) in eight)   // outside
    }

    @Test
    fun `sub-quantum polygon boundary offset is both on and in`() {
        // The boundary pre-pass owns the SCALE tolerance: within one quantum of an edge
        // classifies as on AND in — the winding walk never sees near-boundary noise.
        val nearEdge = FlatPoint(3.0, 2.0 + 1e-9)
        assertTrue(nearEdge on quad)
        assertTrue(nearEdge in quad)
        assertFalse(nearEdge !in quad)
    }

    @Test
    fun `not-in complements in on a concave polygon`() {
        val ell = Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 2.0),
                FlatPoint(2.0, 2.0), FlatPoint(2.0, 4.0), FlatPoint(0.0, 4.0),
            )
        )
        val probes = listOf(
            FlatPoint(1.0, 1.0),   // in the thick part
            FlatPoint(3.0, 3.0),   // in the notch
            FlatPoint(1.0, 3.0),   // in the upright arm
            FlatPoint(2.0, 2.0),   // the notch vertex
            FlatPoint(5.0, 5.0),   // beyond the hull
        )
        for (p in probes) assertEquals(!(p in ell), p !in ell, "complement at $p")
    }

    /*
    RoundPoint in Polygon
    */

    @Test
    fun `round point in polygon mirrors the flat predicate`() {
        assertTrue(RoundPoint(3.0, 4.0) in quad)
        assertTrue(quad contains RoundPoint(3.0, 4.0))
        assertFalse(RoundPoint(6.0, 4.0) in quad)
        assertTrue(RoundPoint(6.0, 4.0) !in quad)
    }

    /*
    ORIENTATION (inside-out polygons)
    */

    @Test
    fun `inside-out polygon boundary is included (closed) - on implies in`() {
        val wall = -quad
        assertTrue(FlatPoint(3.0, 2.0) in wall)   // on bottom edge
        assertTrue(FlatPoint(1.0, 2.0) in wall)   // vertex
    }

    @Test
    fun `strict-interior theorem - not-in the negation is strictly inside`() {
        // p !in -poly ⟺ p strictly inside poly: both orientations are closed, so the boundary
        // is in both, and only the strict interior escapes the complement.
        assertTrue(FlatPoint(3.0, 4.0) !in -quad)    // strict interior
        assertFalse(FlatPoint(3.0, 2.0) !in -quad)   // boundary: in both quad and -quad
        assertFalse(FlatPoint(6.0, 4.0) !in -quad)   // beyond the loop: in -quad's solid region
    }

    @Test
    fun `strict-interior theorem - circle twin`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(FlatPoint(0.5, 0.0) !in -c)    // strict interior
        assertFalse(FlatPoint(1.0, 0.0) !in -c)   // boundary: in both c and -c
        assertFalse(FlatPoint(2.0, 0.0) !in -c)   // beyond the circle: in -c's solid region
    }

    @Test
    fun `round point mirrors the flat predicate on an inside-out polygon`() {
        val wall = -quad
        assertFalse(RoundPoint(3.0, 4.0) in wall)
        assertTrue(RoundPoint(6.0, 4.0) in wall)
    }

    @Test
    fun `inside-out concave polygon contains the notch`() {
        // The L-shape's notch is outside the CCW region, so the inside-out L contains it.
        val ell = -Polygon(
            listOf(
                FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 2.0),
                FlatPoint(2.0, 2.0), FlatPoint(2.0, 4.0), FlatPoint(0.0, 4.0),
            )
        )
        assertTrue(FlatPoint(3.0, 3.0) in ell)    // the notch joins the solid region
        assertFalse(FlatPoint(1.0, 1.0) in ell)   // the thick part is excluded
    }
}
