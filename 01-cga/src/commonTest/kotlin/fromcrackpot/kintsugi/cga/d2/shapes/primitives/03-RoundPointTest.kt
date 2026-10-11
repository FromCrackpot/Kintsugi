package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import kotlin.math.sqrt
import kotlin.test.*

class RoundPointTest {

    /*
    CTOR
     */

    @Test
    fun `4-arg constructor with az=1 is kept as-is`() {
        val r = RoundPoint(1.0, 2.0, 1.0, 2.5)
        assertEquals(1.0, r.ax)
        assertEquals(2.0, r.ay)
        assertEquals(1.0, r.az)
        assertEquals(2.5, r.aw)
    }

    @Test
    fun `4-arg constructor with az=0 produces point at infinity`() {
        val r = RoundPoint(1.0, 2.0, 0.0, 3.0)
        assertEquals(1.0, r.ax)
        assertEquals(2.0, r.ay)
        assertEquals(0.0, r.az)
        assertEquals(3.0, r.aw)
    }

    @Test
    fun `2-arg constructor auto-computes conformal lift`() {
        val r = RoundPoint(3.0, 4.0)
        assertEquals(3.0, r.ax)
        assertEquals(4.0, r.ay)
        assertEquals(1.0, r.az)
        assertEquals(12.5, r.aw)
    }

    /*
    MATH
     */

    @Test
    fun `isAtInfinity is true when az is 0`() {
        assertTrue(RoundPoint(1.0, 2.0, 0.0, 0.0).isAtInfinity)
    }

    @Test
    fun `isAtInfinity is false when az is 1`() {
        assertFalse(RoundPoint(3.0, 4.0).isAtInfinity)
    }

    @Test
    fun `attitude of a finite unitized point is the unit scalar`() {
        // att(a) = a ∨ e412 = az·𝟏
        assertEquals(1.0, RoundPoint(3.0, 4.0).attitude)
    }

    @Test
    fun `attitude of a point at infinity is zero`() {
        assertEquals(0.0, RoundPoint(1.0, 2.0, 0.0, 0.0).attitude)
    }

    @Test
    fun `norm equals sqrt of squared sum`() {
        assertEquals(5.0, RoundPoint(3.0, 4.0).norm)
    }

    @Test
    fun `norm is zero at origin`() {
        assertEquals(0.0, RoundPoint(0.0, 0.0).norm)
    }

    @Test
    fun `toFlat returns FlatPoint with matching xy`() {
        val f = RoundPoint(3.0, 4.0).toFlat
        assertEquals(3.0, f.px)
        assertEquals(4.0, f.py)
        assertEquals(1.0, f.pz)
    }

    @Test
    fun `round-trip through FlatPoint preserves finite point`() {
        val original = RoundPoint(3.0, 4.0)
        assertEquals(original, original.toFlat.toRound)
    }

    @Test
    fun `Euclidean point has zero squared radius`() {
        // For aw = (ax² + ay²)/2, r² = 2·1·aw - ax² - ay² = 0
        val r = RoundPoint(3.0, 4.0)
        assertEquals(0.0, r.radius2)
        assertEquals(0.0, r.radius)
        assertFalse(r.isImaginary)
    }

    @Test
    fun `real round point exposes positive squared radius and magnitude`() {
        // r² = 2·1·5 - 0 - 0 = 10
        val r = RoundPoint(0.0, 0.0, 1.0, 5.0)
        assertEquals(10.0, r.radius2)
        assertEquals(sqrt(10.0), r.radius)
        assertFalse(r.isImaginary)
    }

    @Test
    fun `imaginary round point exposes negative squared radius and isImaginary flag`() {
        // r² = 2·1·(-3) - 0 - 0 = -6
        val r = RoundPoint(0.0, 0.0, 1.0, -3.0)
        assertEquals(-6.0, r.radius2)
        assertEquals(sqrt(6.0), r.radius)
        assertTrue(r.isImaginary)
    }

    @Test
    fun `container of a finite point is the zero-radius circle at the point`() {
        // con(a) = a ∧ car(a)^☆ = Circle(−ax·az, −ay·az, ax²+ay²−az·aw, az²) = (−3, −4, 12.5, 1)
        val container = RoundPoint(3.0, 4.0).container
        assertEquals(Circle(-3.0, -4.0, 12.5, 1.0), container)
        assertEquals(0.0, container.radius2)
        // The container's center is the point itself.
        assertEquals(RoundPoint(3.0, 4.0), container.center)
    }

    @Test
    fun `container of an imaginary round point is an imaginary circle with the same radius2`() {
        // container.radius2 = az²·radius2 — az = 1 preserves it exactly.
        val r = RoundPoint(0.0, 0.0, 1.0, -3.0)
        val container = r.container
        assertEquals(Circle(0.0, 0.0, 3.0, 1.0), container)
        assertEquals(r.radius2, container.radius2)
        assertTrue(container.isImaginary)
    }

    @Test
    fun `container of a point at infinity throws DegenerateCircleException`() {
        // az = 0 collapses the container's round weight cw = az²; the surviving cz = ax²+ay²
        // makes the carried line a line at infinity.
        val ex = assertFailsWith<DegenerateCircleException> { RoundPoint(1.0, 2.0, 0.0, 0.0).container }
        assertEquals(Line(0.0, 0.0, 5.0), ex.line)
    }

    @Test
    fun `sub-quantum negative radius2 is not imaginary`() {
        // r² = 2·(0.5 − 1e-13) − 1 = −2e-13 — negative raw, but below the SCALE quantum.
        // isImaginary is a rounded classification (like isAtInfinity), so FP noise on the
        // null cone classifies real; radius2 itself stays raw data.
        val r = RoundPoint(1.0, 0.0, 1.0, 0.5 - 1e-13)
        assertTrue(r.radius2 < 0.0)
        assertFalse(r.isImaginary)
    }

    @Test
    fun `point difference is the displacement between positions, as a direction`() {
        val diff = RoundPoint(3.0, 4.0) - RoundPoint(1.0, 1.0)
        assertIs<Direction>(diff)
        assertEquals(Direction(2.0, 3.0), diff)
    }

    @Test
    fun `translating by the difference carries the other point onto this one`() {
        val p = RoundPoint(1.0, 1.0)
        val q = RoundPoint(3.0, 4.0)
        assertEquals(q, p * T(q - p))
    }

    @Test
    fun `the round bulk takes no part in the difference`() {
        assertEquals(Direction(2.0, 3.0), RoundPoint(3.0, 4.0, 1.0, 99.0) - RoundPoint(1.0, 1.0))
    }

    @Test
    fun `round directions difference to a flat direction`() {
        assertEquals(Direction(2.0, 2.0), RoundDirection(3.0, 4.0) - RoundDirection(1.0, 2.0))
    }

    /*
    IDENTITY
     */

    @Test
    fun `equal coords yield equal points`() {
        assertEquals(RoundPoint(3.0, 4.0), RoundPoint(3.0, 4.0))
    }

    @Test
    fun `different ax yields unequal points`() {
        assertNotEquals(RoundPoint(3.0, 4.0), RoundPoint(1.0, 4.0))
    }

    @Test
    fun `different ay yields unequal points`() {
        assertNotEquals(RoundPoint(3.0, 4.0), RoundPoint(3.0, 1.0))
    }

    @Test
    fun `different az yields unequal points`() {
        assertNotEquals(
            RoundPoint(3.0, 4.0, 1.0, 12.5),
            RoundPoint(3.0, 4.0, 0.0, 12.5),
        )
    }

    @Test
    fun `different aw yields unequal points`() {
        // The lift of (3, 4) is aw = 12.5; a different aw is a different round point (radius2 ≠ 0).
        assertNotEquals(RoundPoint(3.0, 4.0, 1.0, 99.0), RoundPoint(3.0, 4.0))
    }

    @Test
    fun `equal points have equal hashCode`() {
        assertEquals(RoundPoint(3.0, 4.0).hashCode(), RoundPoint(3.0, 4.0).hashCode())
    }

    @Test
    fun `point is not equal to null or different type`() {
        val r = RoundPoint(3.0, 4.0)
        assertFalse(r.equals(null))
        assertFalse(r.equals("RoundPoint"))
    }

    @Test
    fun `equals is reflexive`() {
        val r = RoundPoint(3.0, 4.0)
        assertTrue(r.equals(r))
    }
}
