package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import kotlin.test.Test
import kotlin.test.assertEquals

class FlatPointTimesTest {

    /* MOTOR — TRANSLATION */

    @Test
    fun `translates the origin by the displacement`() {
        assertEquals(FlatPoint(3.0, 4.0), FlatPoint(0.0, 0.0) * T(3.0, 4.0))
    }

    @Test
    fun `translates a non-origin point by the displacement`() {
        assertEquals(FlatPoint(4.0, 6.0), FlatPoint(1.0, 2.0) * T(3.0, 4.0))
    }

    @Test
    fun `handles negative and zero components`() {
        assertEquals(FlatPoint(-2.0, 6.0), FlatPoint(1.0, 2.0) * T(-3.0, 4.0))
        assertEquals(FlatPoint(1.0, 7.0), FlatPoint(1.0, 2.0) * T(0.0, 5.0))
    }

    @Test
    fun `zero translation is the identity`() {
        val p = FlatPoint(1.0, 2.0)
        assertEquals(p, p * T(0.0, 0.0))
    }

    @Test
    fun `translates a non-unitized point in euclidean space`() {
        // FlatPoint(4, 4, 2) is the euclidean point (2, 2); shifting by (1, 1) lands on (3, 3).
        assertEquals(FlatPoint(3.0, 3.0), FlatPoint(4.0, 4.0, pz = 2.0) * T(1.0, 1.0))
    }

    @Test
    fun `translation leaves a point at infinity unchanged`() {
        // pz = 0 kills the displacement term: a translation must fix directions.
        val v = Direction(3.0, 4.0)
        assertEquals(v, v * T(10.0, 20.0))
    }

    /* MOTOR — ROTATION */

    @Test
    fun `rotates CCW about the origin`() {
        val m = R(FlatPoint(0.0, 0.0), 90.deg)
        assertEquals(FlatPoint(0.0, 1.0), FlatPoint(1.0, 0.0) * m)
        assertEquals(FlatPoint(-1.0, 0.0), FlatPoint(0.0, 1.0) * m)
    }

    @Test
    fun `rotates about a non-origin center`() {
        val m = R(FlatPoint(1.0, 0.0), 90.deg)
        assertEquals(FlatPoint(1.0, 1.0), FlatPoint(2.0, 0.0) * m)
        assertEquals(FlatPoint(1.0, 0.0), FlatPoint(1.0, 0.0) * m) // center is fixed
    }

    @Test
    fun `identity motor is a no-op`() {
        val m = R(FlatPoint(3.0, -4.0), 0.rad)
        assertEquals(FlatPoint(2.5, -7.25), FlatPoint(2.5, -7.25) * m)
    }

    @Test
    fun `a direction rotates position-free`() {
        // The center only shifts positions; a free vector (pz = 0) just turns.
        val m = R(FlatPoint(5.0, 7.0), 90.deg)
        assertEquals(Direction(0.0, 1.0), Direction(1.0, 0.0) * m)
    }

    @Test
    fun `an at-infinity center degrades R to a translator`() {
        // Rotation about a point at infinity is a translation perpendicular to it:
        // 2·tan(θ/2)·(dy, −dx) — for direction (0, 1) and 90°, a translation by (2, 0).
        val m = R(Direction(0.0, 1.0), 90.deg)
        assertEquals(FlatPoint(1.0, 1.0) * T(2.0, 0.0), FlatPoint(1.0, 1.0) * m)
    }

    /* FLECTOR — REFLECTION */

    @Test
    fun `reflects across the x-axis`() {
        assertEquals(FlatPoint(1.0, -2.0), FlatPoint(1.0, 2.0) * F(Line(0.0, 1.0, 0.0)))
    }

    @Test
    fun `reflects across the y-axis`() {
        assertEquals(FlatPoint(-1.0, 2.0), FlatPoint(1.0, 2.0) * F(Line(1.0, 0.0, 0.0)))
    }

    @Test
    fun `reflects across an offset line`() {
        // Line(1, 0, -1) is the vertical line x = 1; the origin lands on (2, 0).
        assertEquals(FlatPoint(2.0, 0.0), FlatPoint(0.0, 0.0) * F(Line(1.0, 0.0, -1.0)))
    }

    @Test
    fun `reflecting across the diagonal swaps coordinates`() {
        // Line(1, -1, 0) is y = x (ctor unitizes the normal).
        assertEquals(FlatPoint(2.0, 1.0), FlatPoint(1.0, 2.0) * F(Line(1.0, -1.0, 0.0)))
    }

    @Test
    fun `leaves a point on the mirror unchanged`() {
        val p = FlatPoint(3.0, 3.0)
        assertEquals(p, p * F(Line(1.0, -1.0, 0.0))) // y = x
    }

    @Test
    fun `reflecting twice across the same line is the identity`() {
        val f = F(Line(1.0, 2.0, 3.0))
        val p = FlatPoint(1.0, 2.0)
        assertEquals(p, p * f * f)
    }

    @Test
    fun `chaining two parallel mirrors is a translation by twice their separation`() {
        // x = 0 then x = 3: reflection pair == translation by (6, 0).
        val p = FlatPoint(1.0, 2.0)
        assertEquals(p * T(6.0, 0.0), p * F(Line(1.0, 0.0, 0.0)) * F(Line(1.0, 0.0, -3.0)))
    }

    @Test
    fun `reflects a non-unitized point in euclidean space`() {
        // FlatPoint(4, 4, 2) is the euclidean point (2, 2); the x-axis mirror lands on (2, -2).
        assertEquals(FlatPoint(2.0, -2.0), FlatPoint(4.0, 4.0, pz = 2.0) * F(Line(0.0, 1.0, 0.0)))
    }

    @Test
    fun `reflects a Direction by flipping its normal component`() {
        // pz = 0 kills the offset term: a reflection acts on a direction as the pure mirror.
        assertEquals(Direction(3.0, -4.0), Direction(3.0, 4.0) * F(Line(0.0, 1.0, -7.0)))
    }

    @Test
    fun `a non-unit mirror reflects like its unitized form`() {
        // Line ctor unitizes: Line(2, 0, 0) is the y-axis.
        assertEquals(FlatPoint(-1.0, 2.0), FlatPoint(1.0, 2.0) * F(Line(2.0, 0.0, 0.0)))
    }

    /* FLECTOR — TRANSFLECTION */

    @Test
    fun `a transflection reflects then glides along the line`() {
        // Mirror = x-axis oriented so its attitude is (1, 0): (2, 5) reflects to (2, −5), then
        // glides by 3 to (5, −5).
        val g = G(Line(0.0, -1.0, 0.0), 3.0)
        assertEquals(FlatPoint(5.0, -5.0), FlatPoint(2.0, 5.0) * g)
    }
}
