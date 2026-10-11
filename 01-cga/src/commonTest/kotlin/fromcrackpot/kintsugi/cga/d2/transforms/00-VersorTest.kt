package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VersorTest {

    /* COMPOSITION — MOTOR * MOTOR */

    @Test
    fun `motor times motor applies the left operand first`() {
        // The load-bearing order contract: `a * b` performs a, then b — which is exactly what
        // makes `*` associative across application: p * (a * b) == (p * a) * b. Uses a
        // non-commuting pair (rotation about a finite center + translation).
        val a = R(FlatPoint(1.0, -2.0), 40.deg)
        val b = T(3.0, 5.0)
        val p = FlatPoint(4.0, 1.0)
        assertEquals((p * a) * b, p * (a * b))
    }

    @Test
    fun `motor composition is not commutative for a rotation and a translation`() {
        val a = R(FlatPoint(0.0, 0.0), 90.deg)
        val b = T(3.0, 0.0)
        val p = FlatPoint(1.0, 0.0)
        // Rotate-then-translate lands elsewhere than translate-then-rotate.
        assertEquals(FlatPoint(3.0, 1.0), p * (a * b))
        assertEquals(FlatPoint(0.0, 4.0), p * (b * a))
    }

    @Test
    fun `composing two translations adds their displacements`() {
        assertEquals(T(8.0, 3.0), T(3.0, 4.0) * T(5.0, -1.0))
    }

    @Test
    fun `composing two co-centred rotations adds their angles`() {
        val c = FlatPoint(2.0, -1.0)
        assertEquals(R(c, 90.deg), R(c, 35.deg) * R(c, 55.deg))
    }

    @Test
    fun `the identity motor is neutral under composition`() {
        val id = Motor(0.0, 0.0, 0.0, 1.0)
        val m = R(FlatPoint(1.0, 2.0), 30.deg)
        assertEquals(m, m * id)
        assertEquals(m, id * m)
    }

    /* COMPOSITION — FLECTOR * FLECTOR (→ MOTOR) */

    @Test
    fun `two reflections across the same line compose to the identity motor`() {
        val f = F(Line(1.0, 2.0, 3.0))
        assertEquals(Motor(0.0, 0.0, 0.0, 1.0), f * f)
    }

    @Test
    fun `two parallel reflections compose to a translation by twice their separation`() {
        // Mirrors x = 0 then x = 3 ⇒ translation by (6, 0).
        assertEquals(T(6.0, 0.0), F(Line(1.0, 0.0, 0.0)) * F(Line(1.0, 0.0, -3.0)))
    }

    @Test
    fun `flector times flector applies the left operand first`() {
        // f1 * f2 is a Motor; applying it must equal reflecting by f1, then by f2.
        val f1 = G(Line(1.0, 0.0, 0.0), 2.0)
        val f2 = G(Line(0.0, 1.0, -1.0), 3.0)
        val p = FlatPoint(2.0, 3.0)
        assertEquals((p * f1) * f2, p * (f1 * f2))
    }

    /* COMPOSITION — MIXED (→ FLECTOR) */

    @Test
    fun `flector times motor applies the left operand first`() {
        val f = G(Line(1.0, 0.0, 0.0), 2.0)
        val m = R(FlatPoint(1.0, -2.0), 40.deg)
        val p = FlatPoint(4.0, 1.0)
        assertEquals((p * f) * m, p * (f * m))
    }

    @Test
    fun `motor times flector applies the left operand first`() {
        val m = R(FlatPoint(1.0, -2.0), 40.deg)
        val f = G(Line(0.0, 1.0, -1.0), 3.0)
        val p = FlatPoint(4.0, 1.0)
        assertEquals((p * m) * f, p * (m * f))
    }

    @Test
    fun `the identity motor is neutral under mixed composition`() {
        val id = Motor(0.0, 0.0, 0.0, 1.0)
        val f = G(Line(1.0, 2.0, 3.0), 1.5)
        assertEquals(f, f * id)
        assertEquals(f, id * f)
    }

    /* UMBRELLA DISPATCH */

    @Test
    fun `versor times versor dispatches to the precise composition`() {
        val m: Versor = R(FlatPoint(1.0, -2.0), 40.deg)
        val f: Versor = G(Line(0.0, 1.0, -1.0), 3.0)
        val composed = m * f
        assertIs<Flector>(composed)
        assertEquals(R(FlatPoint(1.0, -2.0), 40.deg) * G(Line(0.0, 1.0, -1.0), 3.0), composed)
    }

    @Test
    fun `shape times versor dispatches to the precise action`() {
        val p = FlatPoint(4.0, 1.0)
        val m: Versor = T(3.0, 5.0)
        val f: Versor = F(Line(1.0, -1.0, 0.0))
        assertEquals(p * T(3.0, 5.0), p * m)
        assertEquals(p * F(Line(1.0, -1.0, 0.0)), p * f)
    }

    @Test
    fun `a stored composite motion equals its chained applications`() {
        // The call-site contract the whole `*` surface exists for: build a motion once, apply it
        // anywhere, get exactly the chained result — including a mixed (improper) chain.
        val motion = T(2.0, 3.0) * R(FlatPoint(6.0, 4.0), 35.deg) * F(Line(1.0, 2.0, -1.0))
        val p = FlatPoint(1.0, -2.0)
        assertEquals(((p * T(2.0, 3.0)) * R(FlatPoint(6.0, 4.0), 35.deg)) * F(Line(1.0, 2.0, -1.0)), p * motion)
    }

    /* GROUP LAWS THROUGH APPLICATION */

    @Test
    fun `a transflection applied twice is a translation by twice the glide`() {
        // Glide reflection squared = pure translation along the mirror by 2× the glide.
        // Line(0, −1, 0) is the x-axis with attitude (1, 0): glide 3 towards +x.
        val g = G(Line(0.0, -1.0, 0.0), 3.0)
        val p = FlatPoint(2.0, 5.0)
        assertEquals(p * T(6.0, 0.0), (p * g) * g)
        assertEquals(T(6.0, 0.0), g * g)
    }

    @Test
    fun `composing with the inverse translation returns the original point`() {
        val p = FlatPoint(1.0, 2.0)
        assertEquals(p, p * T(3.0, 4.0) * T(-3.0, -4.0))
        assertEquals(p, p * (T(3.0, 4.0) * T(-3.0, -4.0)))
    }

    @Test
    fun `translator motors compose with the scaled-velocity idiom`() {
        // `T(velocity * dt)` — the homogeneous FlatPoint * Double scales the free vector before
        // it becomes the translator's generator.
        val velocity = Direction(30.0, 40.0)
        assertEquals(FlatPoint(4.0, 6.0), FlatPoint(1.0, 2.0) * T(velocity * 0.1))
    }
}
