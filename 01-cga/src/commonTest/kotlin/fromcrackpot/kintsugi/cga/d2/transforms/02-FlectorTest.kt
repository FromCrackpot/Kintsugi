package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateFlectorException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class FlectorTest {

    /* CTOR */

    @Test
    fun `ctor unitizes by the line-normal weight norm`() {
        assertEquals(Flector(0.0, 1.0, 0.0, 0.0), Flector(0.0, 2.0, 0.0, 0.0))
        assertEquals(Flector(0.0, 1.0, 2.0, 3.0), Flector(0.0, 2.0, 4.0, 6.0))
    }

    @Test
    fun `ctor throws on zero weight norm`() {
        // fx = fy = 0 — the mirror line is at infinity, no unitized reflection.
        assertFailsWith<DegenerateFlectorException> { Flector(0.0, 0.0, 5.0, 3.0) }
    }

    /* FACTORY — F */

    @Test
    fun `F is the line with zero glide`() {
        assertEquals(Flector(0.0, 1.0, 0.0, 0.0), F(Line(0.0, 1.0, 0.0)))
        assertEquals(Flector(1.0, 0.0, -3.0, 0.0), F(Line(1.0, 0.0, -3.0)))
    }

    /* FACTORY — G */

    @Test
    fun `G carries the scalar glide as fw = glide over 2`() {
        // The mirror components are the line's own; only the e4 glide coefficient is added.
        assertEquals(Flector(0.0, 1.0, 0.0, 1.5), G(Line(0.0, 1.0, 0.0), 3.0))
    }

    @Test
    fun `G never shifts the mirror`() {
        // The glide is along the line by construction — fz stays the line's own moment
        // (the retired direction-parameter factory could fold a perpendicular part into fz).
        assertEquals(Flector(1.0, 0.0, -3.0, 2.0), G(Line(1.0, 0.0, -3.0), 4.0))
    }

    @Test
    fun `G by zero glide is F`() {
        val line = Line(1.0, 2.0, 3.0)
        assertEquals(F(line), G(line, 0.0))
    }

    @Test
    fun `G glides along the line's own orientation`() {
        // Same point set, opposite orientation: the reflections agree, the glides oppose.
        // Line(0, 1, 0) is the x-axis with attitude (−1, 0); Line(0, −1, 0) has attitude (1, 0).
        val p = FlatPoint(2.0, 5.0)
        assertEquals(FlatPoint(-1.0, -5.0), p * G(Line(0.0, 1.0, 0.0), 3.0))
        assertEquals(FlatPoint(5.0, -5.0), p * G(Line(0.0, -1.0, 0.0), 3.0))
        assertNotEquals(G(Line(0.0, 1.0, 0.0), 3.0), G(Line(0.0, -1.0, 0.0), 3.0))
    }
}
