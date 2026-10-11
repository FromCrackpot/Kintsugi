package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import kotlin.math.sqrt
import kotlin.test.*

class CircleTest {

    /*
    CTOR
     */

    @Test
    fun `4-arg constructor unitizes by absolute value of cw`() {
        val c = Circle(2.0, 4.0, -10.0, -2.0)
        assertEquals(1.0, c.cx)
        assertEquals(2.0, c.cy)
        assertEquals(-5.0, c.cz)
        assertEquals(-1.0, c.cw)
    }

    @Test
    fun `4-arg constructor preserves positive cw orientation after unitization`() {
        val c = Circle(2.0, 4.0, -10.0, 2.0)
        assertEquals(1.0, c.cx)
        assertEquals(2.0, c.cy)
        assertEquals(-5.0, c.cz)
        assertEquals(1.0, c.cw)
    }

    @Test
    fun `4-arg constructor with cw=0 throws DegenerateCircleException carrying remaining components as a Line`() {
        val ex = assertFailsWith<DegenerateCircleException> { Circle(3.0, 4.0, 5.0, 0.0) }
        assertEquals(Line(3.0, 4.0, 5.0), ex.line)
    }

    @Test
    fun `4-arg constructor with all-zero components throws with zero line`() {
        val ex = assertFailsWith<DegenerateCircleException> { Circle(0.0, 0.0, 0.0, 0.0) }
        assertEquals(Line(0.0, 0.0, 0.0), ex.line)
    }

    @Test
    fun `center+radius constructor produces unit cw=-1`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertEquals(-1.0, c.cw)
        // cz = -0.5 * (9 + 16 - 4) = -10.5
        assertEquals(-10.5, c.cz)
        assertEquals(3.0, c.cx)
        assertEquals(4.0, c.cy)
    }

    @Test
    fun `center+radius constructor rejects a negative radius`() {
        assertFailsWith<IllegalArgumentException> { Circle(3.0, 4.0, -2.0) }
    }

    /*
    MATH
     */

    @Test
    fun `radius round-trips through center+radius constructor`() {
        assertEquals(2.0, Circle(3.0, 4.0, 2.0).radius)
    }

    @Test
    fun `radius is correct for unit circle at origin`() {
        assertEquals(1.0, Circle(0.0, 0.0, 1.0).radius)
    }

    @Test
    fun `center is the round point with Euclidean position read from input for classic cw=-1 circle`() {
        // Round center: RoundPoint(-cx·cw, -cy·cw, cw², cx²+cy²−cz·cw) = (3, 4, 1, 14.5).
        // aw = 9 + 16 − (−10.5)(−1) = 14.5 carries the weighted-radius term.
        val c = Circle(3.0, 4.0, 2.0)
        assertEquals(RoundPoint(3.0, 4.0, 1.0, 14.5), c.center)
        assertEquals(FlatPoint(3.0, 4.0), c.center.toFlat)
    }

    @Test
    fun `center Euclidean position is orientation-independent for inside-out cw=+1 circle`() {
        // Same geometric circle as Circle(3,4,2) but inverted orientation:
        // negate all components of the unit cw=-1 form to get cw=+1.
        // Round Center: (-(-3)(1), -(-4)(1), 1, 9+16−10.5) = (3, 4, 1, 14.5) — same as classic.
        val c = Circle(-3.0, -4.0, 10.5, 1.0)
        assertEquals(RoundPoint(3.0, 4.0, 1.0, 14.5), c.center)
        assertEquals(FlatPoint(3.0, 4.0), c.center.toFlat)
    }

    @Test
    fun `norm is the center magnitude sqrt(cx2+cy2)`() {
        assertEquals(5.0, Circle(3.0, 4.0, 2.0).norm)
        // Orientation-independent: the inside-out twin negates cx/cy but the norm is unchanged.
        assertEquals(5.0, Circle(-3.0, -4.0, 10.5, 1.0).norm)
        assertEquals(0.0, Circle(0.0, 0.0, 1.0).norm)
    }

    @Test
    fun `radius is independent of orientation`() {
        val classic = Circle(3.0, 4.0, 2.0)
        val insideOut = Circle(-3.0, -4.0, 10.5, 1.0)
        assertEquals(classic.radius, insideOut.radius)
    }

    @Test
    fun `imaginary circle exposes magnitude radius and isImaginary flag`() {
        // cx² + cy² - 2·cz·cw < 0 → no real points
        val real = Circle(0.0, 0.0, 1.0, -1.0)        // r² = 0 - 2(1)(-1) = 2
        val imaginary = Circle(0.0, 0.0, -1.0, -1.0)  // r² = 0 - 2(-1)(-1) = -2

        assertEquals(2.0, real.radius2)
        assertEquals(sqrt(2.0), real.radius)
        assertFalse(real.isImaginary)

        assertEquals(-2.0, imaginary.radius2)
        assertEquals(sqrt(2.0), imaginary.radius)
        assertTrue(imaginary.isImaginary)
    }

    @Test
    fun `attitude is a dipole at infinity built from cw and the center coords`() {
        // att(c) = c ∨ e412 = −cw·e12 − cy·e41 + cx·e42.
        // Classic unit circle at origin (cw = −1): Dipole(0, 0, 1, 0, 0, 0).
        assertEquals(Dipole(0.0, 0.0, 1.0, 0.0, 0.0, 0.0), Circle(0.0, 0.0, 1.0).attitude)
        // Offset classic circle: center (3, 4) → Dipole(0, 0, 1, −4, 3, 0).
        assertEquals(Dipole(0.0, 0.0, 1.0, -4.0, 3.0, 0.0), Circle(3.0, 4.0, 1.0).attitude)
    }

    @Test
    fun `attitude is at infinity with imaginary unit radius`() {
        val att = Circle(3.0, 4.0, 2.0).attitude
        assertTrue(att.isAtInfinity)
        // radius² = pz² − gz² − 2(gx·py − gy·px) = −cw² = −1, mirroring Dipole.attitude
        assertEquals(-1.0, att.radius2)
        assertTrue(att.isImaginary)
    }

    @Test
    fun `inside-out circle negates the attitude`() {
        // Negating every circle component (orientation flip) negates att(c) componentwise.
        assertEquals(
            Dipole(0.0, 0.0, -1.0, 4.0, -3.0, 0.0),
            Circle(-3.0, -4.0, 10.5, 1.0).attitude, // negation of Circle(3, 4, 2)'s components
        )
    }

    @Test
    fun `carrier is the antiscalar weight cw`() {
        // car(c) = e4 ∧ c = cw·𝟙 — the carrier of a circle is the whole plane.
        assertEquals(-1.0, Circle(3.0, 4.0, 2.0).carrier)
        assertEquals(1.0, Circle(-3.0, -4.0, 10.5, 1.0).carrier)
    }

    @Test
    fun `cocarrier is the flat center for both orientations`() {
        // ccr(c) = e4 ∧ c^☆ = FlatPoint(−cx, −cy, cw) — homogeneous form, unitized by the ctor.
        val classic = Circle(3.0, 4.0, 2.0)
        assertEquals(FlatPoint(3.0, 4.0), classic.cocarrier)
        assertEquals(classic.center.toFlat, classic.cocarrier)

        val insideOut = Circle(-3.0, -4.0, 10.5, 1.0)
        assertEquals(FlatPoint(3.0, 4.0), insideOut.cocarrier)
    }

    @Test
    fun `container is the circle itself normalized to cw=+1`() {
        // con(c) = cw·c — a circle is its own container up to orientation.
        val classic = Circle(3.0, 4.0, 2.0)            // cw = −1
        val insideOut = Circle(-3.0, -4.0, 10.5, 1.0)  // same point set, cw = +1
        assertEquals(insideOut, classic.container)
        assertEquals(insideOut, insideOut.container)
        assertEquals(classic.canonical, classic.container.canonical)
    }

    @Test
    fun `canonical of cw=-1 circle is itself`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertEquals(c, c.canonical)
    }

    @Test
    fun `canonical flips cw=+1 circle to cw=-1`() {
        val c = Circle(-3.0, -4.0, 10.5, 1.0)
        val canon = c.canonical
        assertEquals(-1.0, canon.cw)
        assertEquals(3.0, canon.cx)
        assertEquals(4.0, canon.cy)
        assertEquals(-10.5, canon.cz)
    }

    @Test
    fun `canonical is idempotent`() {
        val c = Circle(-3.0, -4.0, 10.5, 1.0)
        assertEquals(c.canonical, c.canonical.canonical)
    }

    /*
    IDENTITY
     */

    @Test
    fun `equal coords yield equal circles`() {
        assertEquals(Circle(3.0, 4.0, 2.0), Circle(3.0, 4.0, 2.0))
    }

    @Test
    fun `opposing-orientation circles are not equal`() {
        val classic = Circle(3.0, 4.0, 2.0)               // cw = -1
        val insideOut = Circle(-3.0, -4.0, 10.5, 1.0)     // same point set, cw = +1
        assertNotEquals(classic, insideOut)
    }

    @Test
    fun `canonical equates opposing-orientation circles`() {
        val classic = Circle(3.0, 4.0, 2.0)
        val insideOut = Circle(-3.0, -4.0, 10.5, 1.0)
        assertEquals(classic.canonical, insideOut.canonical)
    }

    @Test
    fun `equality tolerates differences within rounding scale`() {
        // SCALE = 10_000 → values agreeing to 1e-4 compare equal.
        val a = Circle(3.0, 4.0, 2.0)
        val b = Circle(3.0 + 1e-6, 4.0, 2.0)
        assertEquals(a, b)
    }

    @Test
    fun `equal circles have equal hashCode`() {
        assertEquals(Circle(3.0, 4.0, 2.0).hashCode(), Circle(3.0, 4.0, 2.0).hashCode())
    }

    @Test
    fun `circle is not equal to null or different type`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertFalse(c.equals(null))
        assertFalse(c.equals("Circle"))
    }

    @Test
    fun `equals is reflexive`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertTrue(c.equals(c))
    }

    /* NEGATION */

    @Test
    fun `isInsideOut reads the orientation sign`() {
        assertFalse(Circle(3.0, 4.0, 2.0).isInsideOut)
        assertFalse(Circle(2.0, 4.0, -10.0, -2.0).isInsideOut)
        assertTrue(Circle(2.0, 4.0, -10.0, 2.0).isInsideOut)
    }

    @Test
    fun `negation swaps the interior, keeping center and radius`() {
        val c = Circle(3.0, 4.0, 2.0)
        val n = -c
        assertTrue(n.isInsideOut)
        assertEquals(c.center, n.center)
        assertEquals(c.radius2, n.radius2)
    }

    @Test
    fun `negation negates all four components`() {
        // Circle(3, 4, 2) stores (3, 4, −10.5, −1).
        assertEquals(Circle(-3.0, -4.0, 10.5, 1.0), -Circle(3.0, 4.0, 2.0))
    }

    @Test
    fun `negation is an involution`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertEquals(c, -(-c))
    }

    @Test
    fun `a circle and its negation compare unequal`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertNotEquals(c, -c)
    }

    @Test
    fun `negation preserves the canonical representative`() {
        val c = Circle(3.0, 4.0, 2.0)
        assertEquals(c.canonical, (-c).canonical)
    }
}
