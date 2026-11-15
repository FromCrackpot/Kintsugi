package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.angle
import fromcrackpot.kintsugi.cga.assertNear
import fromcrackpot.kintsugi.cga.assertSameShape
import fromcrackpot.kintsugi.cga.dilator
import fromcrackpot.kintsugi.cga.flatPoint
import fromcrackpot.kintsugi.cga.scale
import fromcrackpot.kintsugi.cga.shape
import fromcrackpot.kintsugi.cga.sweep
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDilatorException
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DilatorTest {

    /* CTOR */

    @Test
    fun `ctor normalizes so that dw + dz = 1`() {
        assertEquals(Dilator(1.0, 2.0, 0.25, 0.75), Dilator(4.0, 8.0, 1.0, 3.0))
    }

    @Test
    fun `ctor normalization absorbs a negation`() {
        // Dividing by dw + dz, sign included: a dilator and its negation are one value.
        assertEquals(Dilator(1.0, 2.0, 0.25, 0.75), Dilator(-1.0, -2.0, -0.25, -0.75))
    }

    @Test
    fun `ctor throws when dw + dz vanishes`() {
        assertFailsWith<DegenerateDilatorException> { Dilator(1.0, 1.0, 0.5, -0.5) }
    }

    @Test
    fun `ctor throws on a non-positive scale`() {
        assertFailsWith<DegenerateDilatorException> { Dilator(0.0, 0.0, 0.5, 0.5) }  // scale 0
        assertFailsWith<DegenerateDilatorException> { Dilator(0.0, 0.0, 1.5, -0.5) } // scale −2
    }

    /* FACTORY: D */

    @Test
    fun `D builds the dilator from the center and the scale`() {
        // h = (1 − 3)/2 = −1 ⇒ dx = −h·cy = 4, dy = h·cx = −2, dz = h = −1, dw = (1 + 3)/2 = 2.
        assertEquals(Dilator(4.0, -2.0, -1.0, 2.0), D(FlatPoint(2.0, 4.0), 3.0))
    }

    @Test
    fun `D by 1 is the identity dilator`() {
        assertEquals(Dilator(0.0, 0.0, 0.0, 1.0), D(FlatPoint(3.0, -4.0), 1.0))
    }

    @Test
    fun `D throws on a non-positive scale`() {
        assertFailsWith<DegenerateDilatorException> { D(FlatPoint(1.0, 2.0), 0.0) }
        assertFailsWith<DegenerateDilatorException> { D(FlatPoint(1.0, 2.0), -2.0) }
    }

    @Test
    fun `D throws on a non-positive scale about an at-infinity center`() {
        // dz = 0 there, so the ctor alone would accept it as a translator.
        assertFailsWith<DegenerateDilatorException> { D(Direction(1.0, 0.0), -0.5) }
    }

    @Test
    fun `D about an at-infinity center is a translator`() {
        // dz = 0, dw = (1 + σ)/2 ⇒ a translation by 2·(1 − σ)/(1 + σ)·(3, 0) = (2, 0) for σ = 1/2.
        // These are the same components as T(2, 0).
        assertEquals(Dilator(0.0, 1.0, 0.0, 1.0), D(Direction(3.0, 0.0), 0.5))
    }

    /* COMPOSITION */

    @Test
    fun `dilator times dilator applies the left operand first`() {
        val a = D(FlatPoint(0.0, 0.0), 2.0)
        val b = D(FlatPoint(4.0, 0.0), 0.5)
        val p = FlatPoint(1.0, 1.0)
        // a then b: (1, 1) → (2, 2) → (4, 0) + ½·(−2, 2) = (3, 1); b then a: (2.5, 0.5) → (5, 1).
        assertEquals(FlatPoint(3.0, 1.0), p * (a * b))
        assertEquals(FlatPoint(5.0, 1.0), p * (b * a))
    }

    @Test
    fun `dilations whose scales multiply to 1 compose into a translation`() {
        // x ↦ (4, 0) + ½·(2x − (4, 0)) = x + (2, 0).
        assertEquals(Dilator(0.0, 1.0, 0.0, 1.0), D(FlatPoint(0.0, 0.0), 2.0) * D(FlatPoint(4.0, 0.0), 0.5))
    }

    /* IDENTITY */

    @Test
    fun `equal dilators share a hash`() {
        assertEquals(Dilator(1.0, 2.0, 0.25, 0.75).hashCode(), Dilator(4.0, 8.0, 1.0, 3.0).hashCode())
    }

    /* SWEEPS */

    @Test
    fun `co-centred dilations compose by multiplying their scales`() = sweep {
        val c = flatPoint()
        val a = scale()
        val b = scale()
        assertSameDilator(D(c, a * b), D(c, a) * D(c, b))
    }

    @Test
    fun `a composite dilator applies its left operand first on every shape`() = sweep {
        val s = shape()
        val a = dilator()
        val b = dilator()
        assertSameShape(s.dilatedBy(a).dilatedBy(b), s.dilatedBy(a * b))
    }

    @Test
    fun `a dilation commutes with a rotation about the same center on every shape`() = sweep {
        val s = shape()
        val c = flatPoint()
        val dilation = D(c, scale())
        val rotation = R(c, angle())
        assertSameShape(s.dilatedBy(dilation) * rotation, (s * rotation).dilatedBy(dilation))
    }

    private fun Shape.dilatedBy(dilator: Dilator): Shape = when (this) {
        is Primitive -> when (this) {
            is FlatPoint -> this * dilator
            is Line -> this * dilator
            is RoundPoint -> this * dilator
            is Dipole -> this * dilator
            is Circle -> this * dilator
        }
        is Composite -> when (this) {
            is Segment -> this * dilator
            is Polyline -> this * dilator
            is Polygon -> this * dilator
        }
        else -> error("unknown shape category: ${this::class}")
    }

    private fun assertSameDilator(expected: Dilator, actual: Dilator) {
        val e = listOf(expected.dx, expected.dy, expected.dz, expected.dw)
        val a = listOf(actual.dx, actual.dy, actual.dz, actual.dw)
        for (i in e.indices) assertNear(e[i], a[i], "component $i of $actual against $expected")
    }
}
