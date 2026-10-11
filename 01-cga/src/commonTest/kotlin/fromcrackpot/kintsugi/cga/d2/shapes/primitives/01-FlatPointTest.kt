package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import kotlin.test.*

class FlatPointTest {

    /*
    CTOR
     */

    @Test
    fun `2-arg constructor creates unitized euclidean point`() {
        val p = FlatPoint(3.0, 4.0)
        assertEquals(3.0, p.px)
        assertEquals(4.0, p.py)
        assertEquals(1.0, p.pz)
    }

    @Test
    fun `3-arg constructor with pz=1 is kept as-is`() {
        val p = FlatPoint(1.0, 2.0, pz = 1.0)
        assertEquals(1.0, p.px)
        assertEquals(2.0, p.py)
        assertEquals(1.0, p.pz)
    }

    @Test
    fun `3-arg constructor with pz=0 produces point at infinity`() {
        val p = FlatPoint(1.0, 2.0, pz = 0.0)
        assertEquals(1.0, p.px)
        assertEquals(2.0, p.py)
        assertEquals(0.0, p.pz)
    }

    /*
    DIRECTION FACTORY
     */

    @Test
    fun `Direction produces a flat point at infinity`() {
        val d = Direction(3.0, 4.0)
        assertEquals(3.0, d.px)
        assertEquals(4.0, d.py)
        assertEquals(0.0, d.pz)
        assertTrue(d.isAtInfinity)
    }

    @Test
    fun `Direction preserves magnitude (not unitized)`() {
        assertEquals(5.0, Direction(3.0, 4.0).norm)
    }

    @Test
    fun `Direction equals the equivalent at-infinity flat point`() {
        assertEquals(FlatPoint(3.0, 4.0, pz = 0.0), Direction(3.0, 4.0))
    }

    @Test
    fun `Direction toRound is a round point at infinity`() {
        val r = Direction(1.0, 2.0).toRound
        assertEquals(1.0, r.ax)
        assertEquals(2.0, r.ay)
        assertEquals(0.0, r.az)
        assertEquals(0.0, r.aw)
    }

    /*
    MATH
     */

    @Test
    fun `isAtInfinity is true when pz is 0`() {
        assertTrue(FlatPoint(1.0, 2.0, pz = 0.0).isAtInfinity)
    }

    @Test
    fun `isAtInfinity is false when pz is 1`() {
        assertFalse(FlatPoint(1.0, 2.0).isAtInfinity)
    }

    @Test
    fun `norm equals sqrt of squared sum`() {
        assertEquals(5.0, FlatPoint(3.0, 4.0).norm)
    }

    @Test
    fun `norm is zero at origin`() {
        assertEquals(0.0, FlatPoint(0.0, 0.0).norm)
    }

    @Test
    fun `attitude of a finite point is the negated point at infinity`() {
        // att(p) = −pz·e4 → −e4 for a unitized point
        assertEquals(RoundPoint(0.0, 0.0, 0.0, -1.0), FlatPoint(3.0, 4.0).attitude)
    }

    @Test
    fun `attitude is computed on the unitized weight`() {
        // FlatPoint(6, 8, 2) unitizes to pz = 1, so attitude matches the 2-arg form's
        assertEquals(RoundPoint(0.0, 0.0, 0.0, -1.0), FlatPoint(6.0, 8.0, pz = 2.0).attitude)
    }

    @Test
    fun `attitude of a Direction is the zero element`() {
        // A weightless flat (pz = 0) has zero attitude
        assertEquals(RoundPoint(0.0, 0.0, 0.0, 0.0), Direction(3.0, 4.0).attitude)
    }

    @Test
    fun `attitude is a real round point at infinity`() {
        val att = FlatPoint(3.0, 4.0).attitude
        assertTrue(att.isAtInfinity)
        // Zero bulk → radius² = 0 (real), unlike Dipole.attitude's imaginary −1
        assertEquals(0.0, att.radius2)
        assertFalse(att.isImaginary)
    }

    @Test
    fun `toRound on finite point lifts with conformal aw`() {
        val r = FlatPoint(3.0, 4.0).toRound
        assertEquals(3.0, r.ax)
        assertEquals(4.0, r.ay)
        assertEquals(1.0, r.az)
        assertEquals(12.5, r.aw)
    }

    @Test
    fun `toRound on point at infinity has az=0 and aw=0`() {
        val r = FlatPoint(1.0, 2.0, pz = 0.0).toRound
        assertEquals(1.0, r.ax)
        assertEquals(2.0, r.ay)
        assertEquals(0.0, r.az)
        assertEquals(0.0, r.aw)
    }

    @Test
    fun `round-trip through RoundPoint preserves finite point`() {
        val original = FlatPoint(3.0, 4.0)
        assertEquals(original, original.toRound.toFlat)
    }

    @Test
    fun `point difference is the displacement from the other point, as a direction`() {
        val diff = FlatPoint(3.0, 4.0) - FlatPoint(1.0, 1.0)
        assertIs<Direction>(diff)
        assertEquals(Direction(2.0, 3.0), diff)
    }

    @Test
    fun `translating by the difference carries the other point onto this one`() {
        val p = FlatPoint(1.0, 1.0)
        val q = FlatPoint(3.0, 4.0)
        assertEquals(q, p * T(q - p))
    }

    @Test
    fun `equal points difference to the zero direction`() {
        assertEquals(Direction(0.0, 0.0), FlatPoint(3.0, 4.0) - FlatPoint(3.0, 4.0))
    }

    @Test
    fun `a direction operand differences through the same form, as a direction`() {
        val a: FlatPoint = Direction(3.0, 4.0)
        assertIs<Direction>(a - FlatPoint(1.0, 2.0))
        assertEquals(Direction(2.0, 2.0), Direction(3.0, 4.0) - Direction(1.0, 2.0))
    }

    /*
    IDENTITY
     */

    @Test
    fun `equal coords yield equal points`() {
        assertEquals(FlatPoint(1.0, 2.0), FlatPoint(1.0, 2.0))
    }

    @Test
    fun `different coords yield unequal points`() {
        assertNotEquals(FlatPoint(1.0, 2.0), FlatPoint(1.0, 3.0))
    }

    @Test
    fun `equal points have equal hashCode`() {
        assertEquals(FlatPoint(1.0, 2.0).hashCode(), FlatPoint(1.0, 2.0).hashCode())
    }

    @Test
    fun `point is not equal to null or different type`() {
        val p = FlatPoint(1.0, 2.0)
        assertFalse(p.equals(null))
        assertFalse(p.equals("FlatPoint"))
    }

    @Test
    fun `equals is reflexive`() {
        val p = FlatPoint(1.0, 2.0)
        assertTrue(p.equals(p))
    }
}
