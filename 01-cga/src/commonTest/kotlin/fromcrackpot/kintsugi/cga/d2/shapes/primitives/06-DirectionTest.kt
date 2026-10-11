package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteDirectionException
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class DirectionTest {

    /* CTOR */

    @Test
    fun `stores the exact zero weight`() {
        val d = Direction(3.0, 4.0)
        assertEquals(3.0, d.px)
        assertEquals(4.0, d.py)
        assertEquals(0.0, d.pz)
    }

    @Test
    fun `the zero direction is legal`() {
        val d = Direction(0.0, 0.0)
        assertEquals(0.0, d.pz)
        assertEquals(true, d.isAtInfinity)
    }

    /* ANGLE */

    @Test
    fun `angle reads the heading counter-clockwise from positive X`() {
        assertEquals(0.rad, Direction(1.0, 0.0).angle)
        assertEquals(90.deg, Direction(0.0, 1.0).angle)
        assertEquals(180.deg, Direction(-1.0, 0.0).angle)
        assertEquals((-90).deg, Direction(0.0, -1.0).angle)
    }

    @Test
    fun `angle is blind to length and answers zero for the zero direction`() {
        assertEquals(90.deg, Direction(0.0, 2.0).angle)
        assertEquals(0.rad, Direction(0.0, 0.0).angle)
    }

    /* COPY */

    @Test
    fun `copy rebuilds an at-infinity flat point as a direction`() {
        val rebuilt = Direction(1.0, 0.0).copy(FlatPoint(3.0, 4.0, pz = 0.0))
        assertIs<Direction>(rebuilt)
        assertEquals(Direction(3.0, 4.0), rebuilt)
    }

    @Test
    fun `copy rejects a finite flat point`() {
        val finite = FlatPoint(3.0, 4.0)
        val e = assertFailsWith<FiniteDirectionException> { Direction(1.0, 0.0).copy(finite) }
        assertSame(finite, e.point)
    }

    /* IDENTITY */

    @Test
    fun `equals the plain flat point at infinity — both directions`() {
        assertEquals<FlatPoint>(Direction(1.0, 0.0), FlatPoint(1.0, 0.0, pz = 0.0))
        assertEquals<FlatPoint>(FlatPoint(1.0, 0.0, pz = 0.0), Direction(1.0, 0.0))
        assertEquals(FlatPoint(1.0, 0.0, pz = 0.0).hashCode(), Direction(1.0, 0.0).hashCode())
    }

    @Test
    fun `hash-set membership crosses the divide`() {
        assertEquals(true, Direction(1.0, 0.0) in setOf<FlatPoint>(FlatPoint(1.0, 0.0, pz = 0.0)))
    }

    @Test
    fun `never equals a finite flat point`() {
        assertNotEquals<FlatPoint>(Direction(1.0, 0.0), FlatPoint(1.0, 0.0))
    }

    @Test
    fun `other subclasses stay outside the identity domain`() {
        val decorated = object : FlatPoint(1.0, 0.0, 0.0) {}
        assertNotEquals<FlatPoint>(Direction(1.0, 0.0), decorated)
        assertNotEquals<FlatPoint>(decorated, Direction(1.0, 0.0))
    }

    @Test
    fun `toDirection returns itself for a direction`() {
        val d = Direction(3.0, 4.0)
        assertSame(d, d.toDirection)
    }

    @Test
    fun `toDirection canonicalizes a near-zero weight to exact zero`() {
        val converted = FlatPoint(3.0, 4.0, pz = 1e-9).toDirection
        assertEquals(3.0, converted.px)
        assertEquals(4.0, converted.py)
        assertEquals(0.0, converted.pz)
    }

    @Test
    fun `toDirection rejects a finite point`() {
        val finite = FlatPoint(3.0, 4.0)
        val e = assertFailsWith<FiniteDirectionException> { finite.toDirection }
        assertSame(finite, e.point)
    }

    /* MATH */

    @Test
    fun `a translation fixes a direction, as a direction`() {
        val moved = Direction(3.0, 4.0) * T(10.0, 20.0)
        assertIs<Direction>(moved)
        assertEquals(Direction(3.0, 4.0), moved)
    }

    @Test
    fun `a rotation turns a direction, as a direction`() {
        val moved = Direction(1.0, 0.0) * R(FlatPoint(0.0, 0.0), 90.deg)
        assertIs<Direction>(moved)
        assertEquals(Direction(0.0, 1.0), moved)
    }

    @Test
    fun `a flector reflects a direction, as a direction`() {
        val moved = Direction(3.0, 4.0) * F(Line(0.0, 1.0, -7.0))
        assertIs<Direction>(moved)
        assertEquals(Direction(3.0, -4.0), moved)
    }

    @Test
    fun `a versor-typed motion preserves the direction`() {
        val motion: Versor = T(5.0, 0.0)
        assertIs<Direction>(Direction(1.0, 0.0) * motion)
    }

    @Test
    fun `a scalar scales a direction, as a direction`() {
        val scaled = Direction(3.0, 4.0) * 2.0
        assertIs<Direction>(scaled)
        assertEquals(Direction(6.0, 8.0), scaled)
    }

    @Test
    fun `directions add component-wise, as a direction`() {
        val sum = Direction(3.0, 4.0) + Direction(1.0, -2.0)
        assertIs<Direction>(sum)
        assertEquals(Direction(4.0, 2.0), sum)
    }

    @Test
    fun `directions subtract component-wise, as a direction`() {
        val difference = Direction(3.0, 4.0) - Direction(1.0, -2.0)
        assertIs<Direction>(difference)
        assertEquals(Direction(2.0, 6.0), difference)
    }

    @Test
    fun `the zero direction is the additive identity`() {
        assertEquals(Direction(3.0, 4.0), Direction(3.0, 4.0) + Direction(0.0, 0.0))
    }

    @Test
    fun `equal directions cancel to the zero direction`() {
        val cancelled = Direction(3.0, 4.0) - Direction(3.0, 4.0)
        assertEquals(0.0, cancelled.px)
        assertEquals(0.0, cancelled.py)
        assertEquals(0.0, cancelled.pz)
    }

    @Test
    fun `negation reverses the heading, as a direction`() {
        val negated = -Direction(3.0, 4.0)
        assertIs<Direction>(negated)
        assertEquals(Direction(-3.0, -4.0), negated)
    }

    @Test
    fun `negation is an involution`() {
        val d = Direction(3.0, 4.0)
        assertEquals(d, -(-d))
    }

    @Test
    fun `the zero direction is its own negation, sanitized`() {
        val negated = -Direction(0.0, 0.0)
        assertEquals(Direction(0.0, 0.0), negated)
        assertEquals(0.0.toRawBits(), negated.px.toRawBits())
        assertEquals(0.0.toRawBits(), negated.py.toRawBits())
    }
}
