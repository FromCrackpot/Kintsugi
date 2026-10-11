package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import kotlin.math.sqrt
import kotlin.test.*

class MeetTest {

    /*
    Line meet Line => FlatPoint
    */

    @Test
    fun `two crossing lines meet at their intersection`() {
        // y = 0 (Line(0,1,0)) meet x = 0 (Line(1,0,0)) → origin
        assertEquals(FlatPoint(0.0, 0.0), Line(0.0, 1.0, 0.0) meet Line(1.0, 0.0, 0.0))
    }

    @Test
    fun `parallel lines meet at a point at infinity`() {
        // y = 0 meet y = 1 → at-infinity point along x-axis
        val p = Line(0.0, 1.0, 0.0) meet Line(0.0, 1.0, -1.0)
        assertTrue(p.isAtInfinity)
    }

    /*
    Line meet Circle => Dipole
    */

    @Test
    fun `line through circle center yields antipodal dipole`() {
        // y = 0 cuts unit circle at (±1, 0)
        val d = Line(0.0, 1.0, 0.0) meet Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(FlatPoint(0.0, 0.0), d.center.toFlat)
        assertEquals(1.0, d.radius)
        // start/end are the two intersection points (order depends on attitude orientation)
        assertEquals(setOf(RoundPoint(-1.0, 0.0), RoundPoint(1.0, 0.0)), setOf(d.start, d.end))
    }

    @Test
    fun `tangent line yields zero-radius dipole at the tangent point`() {
        // y = 1 is tangent to unit circle at (0, 1) — carrier still has weight, so no throw; radius collapses.
        val d = Line(0.0, 1.0, -1.0) meet Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(FlatPoint(0.0, 1.0), d.center.toFlat)
        assertEquals(0.0, d.radius)
    }

    @Test
    fun `line missing the circle yields imaginary dipole`() {
        // y = 2 misses the unit circle entirely
        val d = Line(0.0, 1.0, -2.0) meet Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue(d.isImaginary)
    }

    @Test
    fun `line-circle and circle-line are orientation-flipped but canonical-equal`() {
        // Antisymmetry: swapping the two grade-3 operands negates every Dipole component
        // (same point set, opposite orientation) — raw equality must fail; canonical must agree.
        val line = Line(0.0, 1.0, 0.0)
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val a = line meet circle
        val b = circle meet line
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    /*
    Circle meet Circle => Dipole
    */

    @Test
    fun `two intersecting circles meet at a real dipole through the intersections`() {
        // Unit circle at origin and unit circle at (1, 0) intersect at (0.5, ±√3/2)
        val d = Circle(0.0, 0.0, 1.0) meet Circle(1.0, 0.0, 1.0)
        assertEquals(FlatPoint(0.5, 0.0), d.center.toFlat)
        assertEquals(sqrt(3.0) / 2.0, d.radius)
        assertEquals(
            setOf(RoundPoint(0.5, -sqrt(3.0) / 2.0), RoundPoint(0.5, sqrt(3.0) / 2.0)),
            setOf(d.start, d.end),
        )
    }

    @Test
    fun `two disjoint circles meet at an imaginary dipole`() {
        // Unit circle at origin and unit circle at (3, 0) — distance 3 > sum of radii 2
        val d = Circle(0.0, 0.0, 1.0) meet Circle(3.0, 0.0, 1.0)
        assertTrue(d.isImaginary)
    }

    @Test
    fun `externally tangent circles yield zero-radius dipole at the tangent point`() {
        // Unit circles centered at (0,0) and (2,0) are externally tangent at (1, 0)
        val d = Circle(0.0, 0.0, 1.0) meet Circle(2.0, 0.0, 1.0)
        assertEquals(FlatPoint(1.0, 0.0), d.center.toFlat)
        assertEquals(0.0, d.radius)
    }

    @Test
    fun `coincident circles collapse to the zero dipole`() {
        // c meet c is antisymmetric → all six components zero → carrier norm zero → ctor throws.
        val ex = assertFailsWith<DegenerateDipoleException> {
            Circle(0.0, 0.0, 1.0) meet Circle(0.0, 0.0, 1.0)
        }
        assertEquals(FlatPoint(0.0, 0.0, 0.0), ex.point)
    }

    @Test
    fun `concentric circles yield an imaginary dipole at infinity`() {
        // gz = cz1·cw2 − cw1·cz2 = 0.5·(−1) − (−1)·2 = 1.5; every other component zero.
        // No real intersection anywhere — the meet lives at infinity (kept raw, not thrown).
        val d = Circle(0.0, 0.0, 1.0) meet Circle(0.0, 0.0, 2.0)
        assertEquals(Dipole(0.0, 0.0, 1.5, 0.0, 0.0, 0.0), d)
        assertTrue(d.isAtInfinity)
        assertTrue(d.isImaginary)
        // The carrier preserves the at-infinity weight — Line's ctor keeps gz raw.
        assertEquals(Line(0.0, 0.0, 1.5), d.carrier)
    }

    @Test
    fun `circle-circle meet is orientation-flipped but canonical-equal under swap`() {
        val c1 = Circle(0.0, 0.0, 1.0)
        val c2 = Circle(1.0, 0.0, 1.0)
        val a = c1 meet c2
        val b = c2 meet c1
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    /*
    Line meet Dipole => RoundPoint
    */

    @Test
    fun `line meets dipole carrier at their intersection point`() {
        // Horizontal dipole on y = 0 (endpoints (-10, 0) and (10, 0)) meets the vertical line x = 0
        // at the origin. Result is the carrier-intersection regardless of segment bounds.
        // aw = 50: the result's radius² = r² − d² = 100 − 0 (minus the power of the point wrt the
        // dipole), real because the intersection lies between the endpoints.
        val dipole = RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0)
        val line = Line(1.0, 0.0, 0.0) // x = 0
        assertEquals(RoundPoint(0.0, 0.0, 1.0, 50.0), line meet dipole)
    }

    @Test
    fun `line meets dipole even when intersection lies outside the dipole's endpoints`() {
        // Dipole on y = 0 with endpoints (-10, 0) and (10, 0); line x = 100 meets the carrier at
        // (100, 0) — well outside the dipole. The pure-CGA result doesn't know about endpoints.
        // aw = 50: radius² = r² − d² = 100 − 100² = −9900 — imaginary because the intersection
        // falls outside the endpoints (aw = lift 5000 + radius²/2).
        val dipole = RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0)
        val line = Line(1.0, 0.0, -100.0) // x = 100
        assertEquals(RoundPoint(100.0, 0.0, 1.0, 50.0), line meet dipole)
    }

    @Test
    fun `parallel line and dipole carrier meet at infinity`() {
        // Both run horizontally — the antiwedge collapses az.
        val dipole = RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0) // carrier: y = 0
        val line = Line(0.0, 1.0, -5.0) // y = 5
        val p = line meet dipole
        assertTrue(p.isAtInfinity)
    }

    /*
    Line meet Segment => RoundPoint?  (bent — null on miss)
    */

    @Test
    fun `line meets segment within bounds returns the intersection`() {
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val line = Line(1.0, 0.0, 0.0) // x = 0 (crosses through midpoint)
        assertEquals(RoundPoint(0.0, 0.0, 1.0, 50.0), line meet segment) // aw: radius² = r² − d² = 100
    }

    @Test
    fun `line meets segment exactly at an endpoint returns the endpoint`() {
        // Closed-interval rule: touching an endpoint counts.
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val line = Line(1.0, 0.0, -10.0) // x = 10
        assertEquals(RoundPoint(10.0, 0.0), line meet segment)
    }

    @Test
    fun `line missing the segment returns null even though the carriers cross`() {
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val line = Line(1.0, 0.0, -100.0) // x = 100 — meets carrier at (100, 0), outside segment
        assertNull(line meet segment)
    }

    @Test
    fun `parallel line and segment return null`() {
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val line = Line(0.0, 1.0, -5.0) // y = 5, parallel to segment
        assertNull(line meet segment)
    }

    @Test
    fun `segment toDipole reverts to pure-CGA carrier-intersection`() {
        // The bent semantics ride on the declared type. Calling segment.toDipole selects the
        // unrestricted overload, which always returns a RoundPoint (here, outside the segment).
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val line = Line(1.0, 0.0, -100.0) // x = 100
        assertEquals(RoundPoint(100.0, 0.0, 1.0, 50.0), line meet segment.toDipole) // radius² = 100 − 100² < 0
        assertNull(line meet segment)
    }

    /*
    Segment meet Segment => RoundPoint?  (bent — null on miss)
    */

    @Test
    fun `two crossing segments meet at their intersection`() {
        val horizontal = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val vertical = Segment(FlatPoint(0.0, -10.0), FlatPoint(0.0, 10.0))
        assertEquals(RoundPoint(0.0, 0.0, 1.0, 50.0), horizontal meet vertical) // aw: radius² = r² − d² = 100
    }

    @Test
    fun `segments sharing an endpoint return the shared point (closed)`() {
        val s1 = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        val s2 = Segment(FlatPoint(10.0, 0.0), FlatPoint(10.0, 10.0))
        assertEquals(RoundPoint(10.0, 0.0), s1 meet s2)
    }

    @Test
    fun `T-junction returns the point on the interior of one and endpoint of the other`() {
        val base = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        val stem = Segment(FlatPoint(5.0, 0.0), FlatPoint(5.0, 10.0))
        assertEquals(RoundPoint(5.0, 0.0), base meet stem)
    }

    @Test
    fun `crossing at an endpoint of a long segment is closed under the flat-δ bounds gate`() {
        // Pins the `on this` bounds gate: closed at the endpoint independent of length L.
        // The retired `between` gate's effective quantum tightened as δ/2L, so long segments
        // are exactly where a future gate change would move the boundary first.
        val long = Segment(FlatPoint(0.0, 0.0), FlatPoint(1000.0, 0.0))
        val atEnd = Segment(FlatPoint(1000.0, -5.0), FlatPoint(1000.0, 5.0))
        // aw is semantic, not the null lift: lift + (r² − d²)/2 against atEnd (r = 5, d = 0).
        assertEquals(RoundPoint(1000.0, 0.0, 1.0, 500012.5), long meet atEnd)
        val pastEnd = Segment(FlatPoint(1000.01, -5.0), FlatPoint(1000.01, 5.0))
        assertNull(long meet pastEnd)
    }

    @Test
    fun `segments whose carriers cross outside both intervals return null`() {
        val s1 = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))      // x ∈ [0,1] on y = 0
        val s2 = Segment(FlatPoint(10.0, -5.0), FlatPoint(10.0, 5.0))    // x = 10, y ∈ [-5, 5]
        // Carriers meet at (10, 0), which lies on s2 but not on s1.
        assertNull(s1 meet s2)
    }

    @Test
    fun `parallel segments return null`() {
        val s1 = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        val s2 = Segment(FlatPoint(0.0, 1.0), FlatPoint(10.0, 1.0))
        assertNull(s1 meet s2)
    }

    @Test
    fun `collinear overlapping segments return null (overlap is not a point)`() {
        // First-impl posture: collinear overlap collapses Line ∨ Line and returns null. A future
        // `Segment overlap Segment → Segment?` would handle the overlap-as-segment case separately.
        val s1 = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        val s2 = Segment(FlatPoint(5.0, 0.0), FlatPoint(15.0, 0.0))
        assertNull(s1 meet s2)
    }
}
