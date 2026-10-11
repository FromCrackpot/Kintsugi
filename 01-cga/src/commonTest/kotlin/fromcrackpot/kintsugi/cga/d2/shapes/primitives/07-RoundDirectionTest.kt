package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteRoundDirectionException
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class RoundDirectionTest {

    /* CTOR */

    @Test
    fun `stores the exact zero round weight`() {
        val d = RoundDirection(3.0, 4.0)
        assertEquals(3.0, d.ax)
        assertEquals(4.0, d.ay)
        assertEquals(0.0, d.az)
        assertEquals(0.0, d.aw)
    }

    @Test
    fun `carries the conformal component`() {
        assertEquals(5.0, RoundDirection(1.0, 2.0, aw = 5.0).aw)
    }

    /* ANGLE */

    @Test
    fun `angle reads the heading counter-clockwise from positive X`() {
        assertEquals(0.rad, RoundDirection(1.0, 0.0).angle)
        assertEquals(90.deg, RoundDirection(0.0, 1.0).angle)
        assertEquals(180.deg, RoundDirection(-1.0, 0.0).angle)
        assertEquals((-90).deg, RoundDirection(0.0, -1.0).angle)
    }

    @Test
    fun `angle ignores the conformal component`() {
        assertEquals(0.rad, RoundDirection(1.0, 0.0, aw = 5.0).angle)
    }

    /* COPY */

    @Test
    fun `copy rebuilds an at-infinity round point as a round direction`() {
        val rebuilt = RoundDirection(1.0, 0.0).copy(RoundPoint(3.0, 4.0, 0.0, 5.0))
        assertIs<RoundDirection>(rebuilt)
        assertEquals(RoundDirection(3.0, 4.0, aw = 5.0), rebuilt)
    }

    @Test
    fun `copy rejects a finite round point`() {
        val finite = RoundPoint(3.0, 4.0)
        val e = assertFailsWith<FiniteRoundDirectionException> { RoundDirection(1.0, 0.0).copy(finite) }
        assertSame(finite, e.point)
    }

    /* IDENTITY */

    @Test
    fun `equals the plain round point at infinity — both directions`() {
        assertEquals<RoundPoint>(RoundDirection(3.0, 4.0, aw = 5.0), RoundPoint(3.0, 4.0, 0.0, 5.0))
        assertEquals<RoundPoint>(RoundPoint(3.0, 4.0, 0.0, 5.0), RoundDirection(3.0, 4.0, aw = 5.0))
        assertEquals(RoundPoint(3.0, 4.0, 0.0, 5.0).hashCode(), RoundDirection(3.0, 4.0, aw = 5.0).hashCode())
    }

    @Test
    fun `never equals a finite round point`() {
        assertNotEquals<RoundPoint>(RoundDirection(3.0, 4.0), RoundPoint(3.0, 4.0))
    }

    @Test
    fun `other subclasses stay outside the identity domain`() {
        val decorated = object : RoundPoint(3.0, 4.0, 0.0, 0.0) {}
        assertNotEquals<RoundPoint>(RoundDirection(3.0, 4.0), decorated)
        assertNotEquals<RoundPoint>(decorated, RoundDirection(3.0, 4.0))
    }

    @Test
    fun `toRoundDirection returns itself for a round direction`() {
        val d = RoundDirection(3.0, 4.0)
        assertSame(d, d.toRoundDirection)
    }

    @Test
    fun `toRoundDirection canonicalizes a near-zero weight to exact zero`() {
        val converted = RoundPoint(3.0, 4.0, 1e-9, 5.0).toRoundDirection
        assertEquals(3.0, converted.ax)
        assertEquals(4.0, converted.ay)
        assertEquals(0.0, converted.az)
        assertEquals(5.0, converted.aw)
    }

    @Test
    fun `toRoundDirection rejects a finite point`() {
        val finite = RoundPoint(3.0, 4.0)
        val e = assertFailsWith<FiniteRoundDirectionException> { finite.toRoundDirection }
        assertSame(finite, e.point)
    }

    /* MATH */

    @Test
    fun `a translation moves the conformal component honestly`() {
        val moved = RoundDirection(1.0, 0.0, aw = 2.0) * T(3.0, 4.0)
        assertIs<RoundDirection>(moved)
        assertEquals(RoundDirection(1.0, 0.0, aw = 5.0), moved)
    }

    @Test
    fun `a rotation turns a round direction, as a round direction`() {
        val moved = RoundDirection(1.0, 0.0) * R(FlatPoint(0.0, 0.0), 90.deg)
        assertIs<RoundDirection>(moved)
        assertEquals(RoundDirection(0.0, 1.0), moved)
    }

    @Test
    fun `a flector reflects a round direction, as a round direction`() {
        val moved = RoundDirection(3.0, 4.0) * F(Line(0.0, 1.0, 0.0))
        assertIs<RoundDirection>(moved)
        assertEquals(RoundDirection(3.0, -4.0), moved)
    }

    @Test
    fun `negation reverses the heading and the conformal component, as a round direction`() {
        val negated = -RoundDirection(3.0, 4.0, 5.0)
        assertIs<RoundDirection>(negated)
        assertEquals(RoundDirection(-3.0, -4.0, -5.0), negated)
    }

    @Test
    fun `negation is an involution`() {
        val d = RoundDirection(3.0, 4.0, 5.0)
        assertEquals(d, -(-d))
    }
}
