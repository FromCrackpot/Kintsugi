package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class JoinTest {

    /*
    FlatPoint join FlatPoint => Line
    */

    @Test
    fun `two distinct euclidean points span the line through them`() {
        // (0,0) join (1,0) → y = 0 line, oriented as Line(0, 1, 0)
        assertEquals(Line(0.0, 1.0, 0.0), FlatPoint(0.0, 0.0) join FlatPoint(1.0, 0.0))
    }

    @Test
    fun `coincident flat points collapse to the zero line`() {
        // Line ctor accepts (0, 0, 0) (no throw); see Line.kt at-infinity branch.
        assertEquals(Line(0.0, 0.0, 0.0), FlatPoint(2.0, 3.0) join FlatPoint(2.0, 3.0))
    }

    @Test
    fun `flat-flat join is orientation-sensitive but canonical-equal under swap`() {
        val a = FlatPoint(0.0, 0.0) join FlatPoint(1.0, 0.0)
        val b = FlatPoint(1.0, 0.0) join FlatPoint(0.0, 0.0)
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    /*
    RoundPoint join RoundPoint => Dipole
    */

    @Test
    fun `two euclidean round points span the dipole between them`() {
        // (-1, 0) join (1, 0) → horizontal dipole through origin, endpoints at ±1 on x-axis.
        // Hand-computed: raw (gx,gy,gz,px,py,pz) = (0, 2, 0, 1, 0, 0); unitized by norm = 2.
        val expected = Dipole(0.0, 1.0, 0.0, 0.5, 0.0, 0.0)
        assertEquals(expected, RoundPoint(-1.0, 0.0) join RoundPoint(1.0, 0.0))
    }

    @Test
    fun `coincident round points throw DegenerateDipoleException with zero point payload`() {
        val ex = assertFailsWith<DegenerateDipoleException> {
            RoundPoint(3.0, 4.0) join RoundPoint(3.0, 4.0)
        }
        // Wedge of a point with itself is the zero bivector → ctor sees all-zero, payload is FlatPoint(0,0,0).
        assertEquals(FlatPoint(0.0, 0.0, 0.0), ex.point)
    }

    @Test
    fun `round-round join is orientation-sensitive but canonical-equal under swap`() {
        val a = RoundPoint(-1.0, 0.0) join RoundPoint(1.0, 0.0)
        val b = RoundPoint(1.0, 0.0) join RoundPoint(-1.0, 0.0)
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    /*
    Dipole join RoundPoint => Circle
    */

    @Test
    fun `three points on the unit circle span that circle`() {
        // (1,0), (0,1), (-1,0) all lie on x² + y² = 1.
        val a = RoundPoint(1.0, 0.0)
        val b = RoundPoint(0.0, 1.0)
        val c = RoundPoint(-1.0, 0.0)
        val expected = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(expected.canonical, ((a join b) join c).canonical)
    }

    @Test
    fun `collinear round points collapse the circle and throw DegenerateCircleException`() {
        // (0,0), (1,0), (2,0) all on y = 0; the circle through them is degenerate (the carrier line).
        val ex = assertFailsWith<DegenerateCircleException> {
            (RoundPoint(0.0, 0.0) join RoundPoint(1.0, 0.0)) join RoundPoint(2.0, 0.0)
        }
        assertEquals(Line(0.0, 1.0, 0.0), ex.line)
    }

    /*
    RoundPoint join Dipole (mirror)
    */

    @Test
    fun `mirror overload produces the same Circle as Dipole join RoundPoint`() {
        // Build a non-trivial dipole and round point; both join orderings should produce equal Circles.
        val dipole = RoundPoint(1.0, 0.0) join RoundPoint(0.0, 1.0)
        val point = RoundPoint(-1.0, 0.0)
        assertEquals(dipole join point, point join dipole)
    }

    @Test
    fun `join is associative on three non-collinear round points modulo orientation`() {
        // (a ∧ b) ∧ c  ==  a ∧ (b ∧ c)  as point sets — compare via canonical.
        val a = RoundPoint(1.0, 0.0)
        val b = RoundPoint(0.0, 1.0)
        val c = RoundPoint(-1.0, 0.0)
        val left = (a join b) join c
        val right = a join (b join c)
        assertEquals(left.canonical, right.canonical)
    }

    /*
    FlatPoint join RoundPoint => Line
    */

    @Test
    fun `flat point and round point span the line through them`() {
        // (0,0) join (1,0) → y = 0 line, oriented as Line(0, 1, 0).
        assertEquals(Line(0.0, 1.0, 0.0), FlatPoint(0.0, 0.0) join RoundPoint(1.0, 0.0))
    }

    @Test
    fun `coincident flat and round points collapse to the zero line`() {
        // Same Euclidean point, different representations — wedge components all vanish.
        assertEquals(Line(0.0, 0.0, 0.0), FlatPoint(2.0, 3.0) join RoundPoint(2.0, 3.0))
    }

    @Test
    fun `flat-round and flat-flat joins agree on Euclidean points`() {
        // Both routes are projective cross products on (px,py,pz) × (ax,ay,az);
        // for unitized Euclidean inputs the two triplets coincide, so the lines do too.
        val flatFlat = FlatPoint(2.0, 3.0) join FlatPoint(5.0, 7.0)
        val flatRound = FlatPoint(2.0, 3.0) join RoundPoint(5.0, 7.0)
        assertEquals(flatFlat, flatRound)
    }

    /*
    RoundPoint join FlatPoint (mirror)
    */

    @Test
    fun `mirror overload produces the same Line as FlatPoint join RoundPoint`() {
        // Grade-1 ∧ grade-2 has commutativity factor (-1)^(1·2) = +1, so they're equal.
        val fp = FlatPoint(2.0, 3.0)
        val rp = RoundPoint(5.0, 7.0)
        assertEquals(fp join rp, rp join fp)
    }
}
