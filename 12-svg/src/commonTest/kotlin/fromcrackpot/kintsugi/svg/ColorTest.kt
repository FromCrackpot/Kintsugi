package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.svg.parsing.parseColor
import fromcrackpot.kintsugi.svg.serializing.formatHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorTest {

    /*
    PARSE
     */

    @Test
    fun `parseColor accepts 3-digit hex`() {
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), parseColor("#f00"))
    }

    @Test
    fun `parseColor accepts 6-digit hex`() {
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), parseColor("#FF0000"))
    }

    @Test
    fun `parseColor accepts 4-digit hex with alpha`() {
        assertEquals(ShapeColor.rgb(0x11, 0x22, 0x33, 0x44), parseColor("#1234"))
    }

    @Test
    fun `parseColor accepts 8-digit hex with alpha`() {
        assertEquals(ShapeColor.rgb(0x11, 0x22, 0x33, 0x44), parseColor("#11223344"))
    }

    @Test
    fun `parseColor accepts rgb() with int components`() {
        assertEquals(ShapeColor.rgb(0, 128, 255), parseColor("rgb(0, 128, 255)"))
    }

    @Test
    fun `parseColor accepts rgba() with float alpha`() {
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00, 0x80), parseColor("rgba(255, 0, 0, 0.5)"))
    }

    @Test
    fun `parseColor accepts none as ShapeColor NONE`() {
        assertEquals(ShapeColor.NONE, parseColor("none"))
        assertEquals(ShapeColor.NONE, parseColor("NONE"))
        assertEquals(ShapeColor.NONE, parseColor("  none  "))
    }

    @Test
    fun `parseColor returns null for unrecognized input`() {
        assertNull(parseColor("currentColor"))
        assertNull(parseColor("hsl(0, 100%, 50%)"))
        assertNull(parseColor(""))
        assertNull(parseColor("#xyz"))
    }

    /*
    FORMAT
     */

    @Test
    fun `formatHex emits 6-digit form when fully opaque`() {
        assertEquals("#ff0000", ShapeColor.rgb(0xFF, 0x00, 0x00).formatHex())
    }

    @Test
    fun `formatHex emits 8-digit form when alpha is non-opaque`() {
        assertEquals("#11223344", ShapeColor.rgb(0x11, 0x22, 0x33, 0x44).formatHex())
    }

    @Test
    fun `formatHex round-trips through parseColor`() {
        val c = ShapeColor.rgb(0x12, 0x34, 0x56, 0x78)
        assertEquals(c, parseColor(c.formatHex()))
    }

    @Test
    fun `formatHex emits none for any zero-alpha color`() {
        assertEquals("none", ShapeColor.NONE.formatHex())
        // Canonicalization: any color with alpha=0 collapses to ShapeColor.NONE and emits "none".
        assertEquals("none", ShapeColor.rgb(0x12, 0x34, 0x56, 0x00).formatHex())
    }

    @Test
    fun `none round-trips through parseColor and formatHex`() {
        assertEquals("none", ShapeColor.NONE.formatHex())
        assertEquals(ShapeColor.NONE, parseColor("none"))
        assertEquals(ShapeColor.NONE, parseColor("#00000000"))
    }
}
