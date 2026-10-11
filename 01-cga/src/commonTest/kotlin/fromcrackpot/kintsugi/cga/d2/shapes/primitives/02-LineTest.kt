package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import kotlin.test.*

class LineTest {

    /*
    CTOR
     */

    @Test
    fun `constructor unitizes by gradient magnitude`() {
        val l = Line(3.0, 4.0, 5.0)
        assertEquals(0.6, l.gx)
        assertEquals(0.8, l.gy)
        assertEquals(1.0, l.gz)
    }

    @Test
    fun `already-unit line passes through unchanged`() {
        val l = Line(1.0, 0.0, 7.0)
        assertEquals(1.0, l.gx)
        assertEquals(0.0, l.gy)
        assertEquals(7.0, l.gz)
    }

    @Test
    fun `line at infinity keeps gz raw (weight is data, not unitized)`() {
        val l = Line(0.0, 0.0, 5.0)
        assertEquals(0.0, l.gx)
        assertEquals(0.0, l.gy)
        assertEquals(5.0, l.gz)
    }

    @Test
    fun `line at infinity keeps negative gz raw`() {
        val l = Line(0.0, 0.0, -3.0)
        assertEquals(0.0, l.gx)
        assertEquals(0.0, l.gy)
        assertEquals(-3.0, l.gz)
    }

    @Test
    fun `degenerate zero line stays zero`() {
        val l = Line(0.0, 0.0, 0.0)
        assertEquals(0.0, l.gx)
        assertEquals(0.0, l.gy)
        assertEquals(0.0, l.gz)
    }

    @Test
    fun `signed-zero inputs are sanitized to positive zero`() {
        val l = Line(-0.0, -0.0, 1.0)
        assertEquals(0.0, l.gx)
        assertEquals(0.0, l.gy)
        // bit-level check: 0.0 and -0.0 both equal 0.0 by ==, but their bits differ
        assertEquals(0.0.toRawBits(), l.gx.toRawBits())
        assertEquals(0.0.toRawBits(), l.gy.toRawBits())
    }

    /*
    MATH
     */

    @Test
    fun `isAtInfinity is true for line at infinity`() {
        assertTrue(Line(0.0, 0.0, 1.0).isAtInfinity)
    }

    @Test
    fun `isAtInfinity is false for finite line`() {
        assertFalse(Line(1.0, 0.0, 0.0).isAtInfinity)
    }

    @Test
    fun `norm is the absolute flat bulk gz`() {
        assertEquals(7.0, Line(1.0, 0.0, 7.0).norm)
        assertEquals(7.0, Line(1.0, 0.0, -7.0).norm)
        // Unitization divides gz by the gradient magnitude: |5| / 5 = 1
        assertEquals(1.0, Line(3.0, 4.0, 5.0).norm)
    }

    @Test
    fun `normal is the gradient as a direction`() {
        assertEquals(Direction(1.0, 0.0), Line(1.0, 0.0, 7.0).normal)
        // Unitized gradient: (3, 4) / 5
        assertEquals(Direction(0.6, 0.8), Line(3.0, 4.0, 5.0).normal)
    }

    @Test
    fun `attitude is the carrier tangent, 90 degrees CCW from the normal`() {
        // att(g) = g ∨ e412 = −gy·e41 + gx·e42: Line(1,0,0) is the vertical
        // line x=0 with normal (1,0) → tangent direction (0,1)
        assertEquals(Direction(0.0, 1.0), Line(1.0, 0.0, 0.0).attitude)
    }

    @Test
    fun `attitude of the line at infinity is the zero direction`() {
        assertEquals(Direction(0.0, 0.0), Line(0.0, 0.0, 1.0).attitude)
    }

    @Test
    fun `canonical flips when first nonzero gradient component is negative`() {
        val l = Line(-1.0, 0.0, 2.0)
        val c = l.canonical
        assertEquals(1.0, c.gx)
        assertEquals(0.0, c.gy)
        assertEquals(-2.0, c.gz)
    }

    @Test
    fun `canonical uses gy when gx is zero`() {
        val l = Line(0.0, -1.0, 3.0)
        val c = l.canonical
        assertEquals(0.0, c.gx)
        assertEquals(1.0, c.gy)
        assertEquals(-3.0, c.gz)
    }

    @Test
    fun `canonical does not flip when first nonzero is positive`() {
        val l = Line(1.0, 0.0, 2.0)
        assertEquals(l, l.canonical)
    }

    @Test
    fun `canonical of an at-infinity line is sign- and scale-blind`() {
        // The at-infinity point set is unique — any non-zero gz maps to Line(0, 0, 1).
        assertEquals(Line(0.0, 0.0, 1.0), Line(0.0, 0.0, 5.0).canonical)
        assertEquals(Line(0.0, 0.0, 1.0), Line(0.0, 0.0, -3.0).canonical)
    }

    @Test
    fun `canonical of the degenerate zero line is itself`() {
        val l = Line(0.0, 0.0, 0.0)
        assertEquals(l, l.canonical)
    }

    @Test
    fun `canonical is idempotent at infinity`() {
        val l = Line(0.0, 0.0, -3.0)
        assertEquals(l.canonical, l.canonical.canonical)
    }

    @Test
    fun `canonical is stable under FP-noise-only negativity`() {
        // After unitization gx ≈ -1e-17 (rounds to 0 at SCALE=10_000), gy ≈ 1 (positive).
        // Canonical decides sign from _gx, _gy (rounded): _gx == 0 → falls through to _gy.sign = +1.
        // The raw-value implementation (gx.sign) would have flipped on the FP noise alone.
        val l = Line(-1e-17, 1.0, 0.0)
        assertEquals(l, l.canonical)
    }

    @Test
    fun `canonical is idempotent`() {
        val l = Line(-1.0, 2.0, 3.0)
        assertEquals(l.canonical, l.canonical.canonical)
    }

    /*
    IDENTITY
     */

    @Test
    fun `equal coords yield equal lines`() {
        assertEquals(Line(1.0, 0.0, 2.0), Line(1.0, 0.0, 2.0))
    }

    @Test
    fun `opposing-orientation lines are not equal`() {
        // Same point set, opposite normals: equals must reject.
        assertNotEquals(Line(1.0, 0.0, 2.0), Line(-1.0, 0.0, -2.0))
    }

    @Test
    fun `canonical equates opposing-orientation lines`() {
        val a = Line(1.0, 0.0, 2.0)
        val b = Line(-1.0, 0.0, -2.0)
        assertEquals(a.canonical, b.canonical)
    }

    @Test
    fun `equality tolerates differences within rounding scale`() {
        // SCALE = 10_000, so values agreeing to 1e-4 should compare equal.
        val a = Line(1.0, 0.0, 2.0)
        val b = Line(1.0 + 1e-6, 0.0, 2.0 + 1e-6)
        assertEquals(a, b)
    }

    @Test
    fun `equal lines have equal hashCode`() {
        assertEquals(Line(1.0, 0.0, 2.0).hashCode(), Line(1.0, 0.0, 2.0).hashCode())
    }

    @Test
    fun `line is not equal to null or different type`() {
        val l = Line(1.0, 0.0, 2.0)
        assertFalse(l.equals(null))
        assertFalse(l.equals("Line"))
    }

    @Test
    fun `equals is reflexive`() {
        val l = Line(1.0, 0.0, 2.0)
        assertTrue(l.equals(l))
    }

    @Test
    fun `toStringRounded uses rounded values`() {
        val l = Line(1.0, 0.0, 2.0)
        assertEquals("Line(gx=1.0, gy=0.0, gz=2.0)", l.toStringRounded())
    }

    /* NEGATION */

    @Test
    fun `negation flips the normal and the signed distance`() {
        assertEquals(Line(-3.0, -4.0, -5.0), -Line(3.0, 4.0, 5.0))
    }

    @Test
    fun `negation is an involution`() {
        val l = Line(3.0, 4.0, 5.0)
        assertEquals(l, -(-l))
    }

    @Test
    fun `a line and its negation compare unequal`() {
        val l = Line(3.0, 4.0, 5.0)
        assertNotEquals(l, -l)
    }

    @Test
    fun `negation preserves the canonical representative`() {
        val l = Line(3.0, 4.0, 5.0)
        assertEquals(l.canonical, (-l).canonical)
    }

    @Test
    fun `negation of a line at infinity negates the magnitude`() {
        assertEquals(Line(0.0, 0.0, -5.0), -Line(0.0, 0.0, 5.0))
    }
}
