package fromcrackpot.kintsugi.cga.d2.queries.measures

import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class AngleTest {

    private val EPS = 1e-12

    // Directions read off the attitude (−gy, gx).
    private val dirX = Line(0.0, -1.0, 0.0)   // runs along +x
    private val dirY = Line(1.0, 0.0, 0.0)    // runs along +y
    private val dir45 = Line(1.0, -1.0, 0.0)  // runs along (1, 1)

    /*
    LINE
     */

    @Test
    fun `perpendicular lines answer a quarter turn, CCW positive`() {
        assertEquals(PI / 2, (dirX angleTo dirY).radians, EPS)
        assertEquals(-PI / 2, (dirY angleTo dirX).radians, EPS)
    }

    @Test
    fun `a line to itself answers zero`() {
        assertEquals(0.0, (dirX angleTo dirX).radians)
    }

    @Test
    fun `parallel same-direction lines answer zero`() {
        assertEquals(0.0, (Line(0.0, -1.0, 0.0) angleTo Line(0.0, -1.0, 5.0)).radians)
    }

    @Test
    fun `opposite orientations answer the positive half turn`() {
        assertEquals(PI, (dirX angleTo Line(0.0, 1.0, 0.0)).radians, EPS)
        // Signed zeros canonicalize: the half turn never answers −π.
        assertEquals(PI, (Line(0.0, 1.0, 0.0) angleTo Line(0.0, -1.0, 0.0)).radians, EPS)
    }

    @Test
    fun `a diagonal answers an eighth turn`() {
        assertEquals(PI / 4, (dirX angleTo dir45).radians, EPS)
    }

    @Test
    fun `the angle is antisymmetric`() {
        assertEquals((dirX angleTo dir45).radians, -(dir45 angleTo dirX).radians, EPS)
    }

    @Test
    fun `a rigid motion preserves the angle`() {
        val rot = R(FlatPoint(2.0, 1.0), 30.deg)
        assertEquals(
            (dirX angleTo dir45).radians,
            ((dirX * rot) angleTo (dir45 * rot)).radians,
            EPS
        )
    }

    @Test
    fun `a reflection negates the angle`() {
        val mirror = F(Line(1.0, 2.0, -3.0))
        assertEquals(
            -(dirX angleTo dir45).radians,
            ((dirX * mirror) angleTo (dir45 * mirror)).radians,
            EPS
        )
    }

    @Test
    fun `an at-infinity operand degenerates to the zero angle`() {
        assertEquals(0.0, (Line(0.0, 0.0, 5.0) angleTo dirX).radians)
        assertEquals(0.0, (Line(0.0, 0.0, 5.0) angleTo Line(-1.0, -1.0, 0.0)).radians)
        assertEquals(0.0, (dirX angleTo Line(0.0, 0.0, 5.0)).radians)
    }
}
