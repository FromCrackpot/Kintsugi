package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import kotlin.test.*

class SegmentTest {

    /*
    CTOR
     */

    @Test
    fun `segment from two endpoints matches the wedge dipole component-for-component`() {
        // The convenience ctor lifts each endpoint to a RoundPoint and wedges them. The resulting
        // 6 components must match the explicit `RoundPoint join RoundPoint` Dipole.
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        val segment = Segment(a, b)
        val dipole = a.toRound join b.toRound
        assertEquals(dipole.gx, segment.gx)
        assertEquals(dipole.gy, segment.gy)
        assertEquals(dipole.gz, segment.gz)
        assertEquals(dipole.px, segment.px)
        assertEquals(dipole.py, segment.py)
        assertEquals(dipole.pz, segment.pz)
    }

    @Test
    fun `segment from two RoundPoints matches the join dipole component-for-component`() {
        // The (RoundPoint, RoundPoint) ctor wedges directly without the FlatPoint→RoundPoint
        // lift step — the components must agree with the explicit `join` Dipole.
        val a = RoundPoint(-10.0, 0.0)
        val b = RoundPoint(10.0, 0.0)
        val segment = Segment(a, b)
        val dipole = a join b
        assertEquals(dipole.gx, segment.gx)
        assertEquals(dipole.gy, segment.gy)
        assertEquals(dipole.gz, segment.gz)
        assertEquals(dipole.px, segment.px)
        assertEquals(dipole.py, segment.py)
        assertEquals(dipole.pz, segment.pz)
    }

    @Test
    fun `segment exposes start and end as the dipole's contained round points`() {
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        // start/end are the two contained round points of the underlying blade. The dipole's
        // attitude convention puts start = center − r·attitude, end = center + r·attitude — for
        // a horizontal dipole that means start is the (+x) endpoint, end the (−x) one.
        assertEquals(setOf(RoundPoint(-10.0, 0.0), RoundPoint(10.0, 0.0)), setOf(segment.start, segment.end))
    }

    @Test
    fun `RoundPoint ctor caches its operands - start and end return the very instances`() {
        // Endpoint caching: start/end are the ctor operands by reference, not the Dipole-derived
        // reconstructions (center ± r·attitude), which are only rounded-equal to the inputs.
        val a = RoundPoint(-10.0, 0.0)
        val b = RoundPoint(10.0, 0.0)
        val segment = Segment(a, b)
        assertSame(a, segment.start)
        assertSame(b, segment.end)
    }

    @Test
    fun `FlatPoint ctor caches the lifted endpoints - start and end return the toRound instances`() {
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        val segment = Segment(a, b)
        // FlatPoint.toRound is a lazy val, so the lift instance is stable across calls.
        assertSame(a.toRound, segment.start)
        assertSame(b.toRound, segment.end)
    }

    /*
    ORDERING
     */

    @Test
    fun `reversing endpoints yields a non-equal segment (sign-flipped blade)`() {
        // Segment(A, B) and Segment(B, A) lift to negated 6-component blades, so component-strict
        // equality fails — the orientation is meaningful (matches SVG x1,y1 → x2,y2 fidelity).
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        assertNotEquals(Segment(a, b), Segment(b, a))
    }

    @Test
    fun `canonical collapses the two reversed segments`() {
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        assertEquals(Segment(a, b).canonical, Segment(b, a).canonical)
    }

    @Test
    fun `canonical returns a Segment, not a bare Dipole (covariant return)`() {
        val flipped = Segment(FlatPoint(10.0, 0.0), FlatPoint(-10.0, 0.0))
        // Static type of `canonical` on a Segment receiver is Segment — assignment to Segment
        // compiles without cast, and the runtime class is Segment.
        val canonical: Segment = flipped.canonical
        assertEquals(Segment::class, canonical::class)
    }

    @Test
    fun `canonical swaps the cached endpoints when the blade flips`() {
        // Negating the blade exchanges start/end exactly (center/radius are quadratic in the
        // components, attitude is linear), so the flipped Segment carries the same endpoint
        // instances with the roles swapped. Pick the non-canonical ordering at runtime so the
        // test doesn't pin the wedge sign convention.
        val a = RoundPoint(10.0, 0.0)
        val b = RoundPoint(-10.0, 0.0)
        val ab = Segment(a, b)
        val nonCanonical = if (ab.canonical === ab) Segment(b, a) else ab
        assertSame(nonCanonical.end, nonCanonical.canonical.start)
        assertSame(nonCanonical.start, nonCanonical.canonical.end)
    }

    /*
    IDENTITY
     */

    @Test
    fun `dipole is never equal to a segment with matching components (different types)`() {
        // Segment is not a Dipole subtype — under composition, the algebra/composite boundary is a
        // private-field invariant, so equality across the types falls out for free.
        val a = RoundPoint(-10.0, 0.0)
        val b = RoundPoint(10.0, 0.0)
        val dipole: Dipole = a join b
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        assertNotEquals<Any>(dipole, segment)
        assertNotEquals<Any>(segment, dipole)
    }

    @Test
    fun `two segments with matching components are equal`() {
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        assertEquals(Segment(a, b), Segment(a, b))
    }

    @Test
    fun `equal segments have equal hashCode`() {
        val a = FlatPoint(-10.0, 0.0)
        val b = FlatPoint(10.0, 0.0)
        assertEquals(Segment(a, b).hashCode(), Segment(a, b).hashCode())
    }

    /*
    BRIDGES (toDipole / toSegment)
     */

    @Test
    fun `toDipole yields the inner Dipole - same instance, equal to a freshly built Dipole`() {
        val segment = Segment(FlatPoint(-10.0, 0.0), FlatPoint(10.0, 0.0))
        val asDipole: Dipole = segment.toDipole
        // Runtime class is exactly Dipole.
        assertEquals(Dipole::class, asDipole::class)
        // Zero-allocation contract: repeated calls return the held instance.
        assertSame(segment.toDipole, segment.toDipole)
        // Components agree component-for-component.
        assertEquals(segment.gx, asDipole.gx)
        assertEquals(segment.gy, asDipole.gy)
        assertEquals(segment.gz, asDipole.gz)
        assertEquals(segment.px, asDipole.px)
        assertEquals(segment.py, asDipole.py)
        assertEquals(segment.pz, asDipole.pz)
        // Different types are never equal — Segment is not a Dipole subtype.
        assertNotEquals<Any>(segment, asDipole)
        // But toDipole == another Dipole built the same way.
        val joinDipole = RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0)
        assertEquals(joinDipole, asDipole)
    }

    @Test
    fun `Dipole toSegment yields a Segment matching Segment from the same RoundPoints`() {
        val a = RoundPoint(-10.0, 0.0)
        val b = RoundPoint(10.0, 0.0)
        val dipole = a join b
        val promoted: Segment = dipole.toSegment
        // Promoted runtime class is Segment.
        assertEquals(Segment::class, promoted::class)
        // Equal to the directly-built Segment with the same endpoints.
        assertEquals(Segment(a, b), promoted)
        // Different types are never equal in the reverse direction either.
        assertNotEquals<Any>(dipole, promoted)
    }

    @Test
    fun `toSegment path has no cached endpoints - start and end fall back to the derived points`() {
        // Promoting a bare Dipole carries no original endpoint instances; start/end are the
        // Dipole-derived reconstructions — rounded-equal to the join operands, not identical.
        val a = RoundPoint(-10.0, 0.0)
        val b = RoundPoint(10.0, 0.0)
        val promoted = (a join b).toSegment
        assertEquals(setOf(a, b), setOf(promoted.start, promoted.end))
    }

    /*
    FORWARDED OPERATIONS
     */

    @Test
    fun `segment carrier is the line through start and end`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(0.0, 10.0))
        // Vertical segment → carrier is x = 0 → Line(±1, 0, 0). Orientation depends on which
        // wedge-side `start` lifts to; canonical compares the geometric line itself.
        assertEquals(Line(1.0, 0.0, 0.0).canonical, segment.carrier.canonical)
    }

    @Test
    fun `segment container is the circle having the endpoints as a diameter`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(0.0, 10.0))
        // Forwarded from the inner dipole: center (0, 5), radius 5. The container comes out
        // cw = +1 (inside-out) from the algebra; canonical compares the geometric circle.
        assertEquals(Circle(centerX = 0.0, centerY = 5.0, radius = 5.0).canonical, segment.container.canonical)
        assertTrue(segment.start on segment.container)
        assertTrue(segment.end on segment.container)
    }

    @Test
    fun `segment joined with a round point yields a circle through all three points`() {
        // Composite-side forwarder: `Segment join RoundPoint` delegates through `toDipole` so the
        // composite type is first-class without `.toDipole` ceremony at the call site. Wedge
        // antisymmetry is even on g1 ∧ g2, so the symmetric direction yields the same circle.
        val segment = Segment(FlatPoint(1.0, 0.0), FlatPoint(-1.0, 0.0))
        val third = RoundPoint(0.0, 1.0)
        val expected = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0).canonical
        assertEquals(expected, (segment join third).canonical)
        assertEquals(expected, (third join segment).canonical)
    }

    @Test
    fun `segment is not imaginary by construction`() {
        // Lifting two real, finite FlatPoints and wedging them always yields a real blade.
        val segment = Segment(FlatPoint(1.0, 2.0), FlatPoint(4.0, -3.0))
        assertTrue(!segment.isImaginary)
    }

    @Test
    fun `radius stays robust at large coordinates (difference-first path)`() {
        // The Dipole-forwarded radius² squares wedge components carrying O(coord³·ε) error — at
        // ~10⁷ it returns garbage (−26108 here) and goes spuriously imaginary. Generic positions
        // that corrupted also fail the ctor's Plücker gate (loud), but exact-cancellation
        // alignments like this axis-aligned segment construct fine — the silent corridor the
        // difference-first path must cover.
        val segment = Segment(FlatPoint(1e7, 0.0), FlatPoint(1e7 + 4.0, 0.0))
        assertEquals(4.0, segment.radius2)
        assertEquals(2.0, segment.radius)
        assertTrue(!segment.isImaginary)
    }

    @Test
    fun `length is the distance between the endpoints - twice the radius`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(3.0, 4.0))
        assertEquals(5.0, segment.length)
        assertEquals(segment.radius * 2, segment.length)
    }

    /* NEGATION */

    @Test
    fun `negation exchanges the endpoint instances`() {
        val s = Segment(RoundPoint(0.0, 0.0), RoundPoint(3.0, 4.0))
        val n = -s
        assertSame(s.start, n.end)
        assertSame(s.end, n.start)
    }

    @Test
    fun `negation agrees with the dipole's - wedge antisymmetry`() {
        val s = Segment(FlatPoint(1.0, 2.0), FlatPoint(5.0, -1.0))
        assertEquals(-(s.toDipole), (-s).toDipole)
    }

    @Test
    fun `negation is an involution`() {
        val s = Segment(FlatPoint(1.0, 2.0), FlatPoint(5.0, -1.0))
        assertEquals(s, -(-s))
    }

    @Test
    fun `a segment and its negation compare unequal`() {
        val s = Segment(FlatPoint(1.0, 2.0), FlatPoint(5.0, -1.0))
        assertNotEquals(s, -s)
    }

    @Test
    fun `negation preserves the canonical representative`() {
        val s = Segment(FlatPoint(1.0, 2.0), FlatPoint(5.0, -1.0))
        assertEquals(s.canonical, (-s).canonical)
    }

    @Test
    fun `negation of a segment at infinity negates the blade directly`() {
        val inf = Dipole(0.0, 0.0, 5.0, 1.0, 2.0, 0.0).toSegment
        assertEquals(Dipole(0.0, 0.0, -5.0, -1.0, -2.0, 0.0), (-inf).toDipole)
    }
}
