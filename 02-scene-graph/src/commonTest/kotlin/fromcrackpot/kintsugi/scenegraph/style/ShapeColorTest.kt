package fromcrackpot.kintsugi.scenegraph.style

import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeColorTest {

    /*
    PACKING
     */

    @Test
    fun `rgb factory packs components into argb`() {
        val c = ShapeColor.rgb(0x11, 0x22, 0x33)
        assertEquals(0x11, c.r)
        assertEquals(0x22, c.g)
        assertEquals(0x33, c.b)
        assertEquals(0xFF, c.a)
    }

    @Test
    fun `rgb factory accepts explicit alpha`() {
        val c = ShapeColor.rgb(0x11, 0x22, 0x33, 0x44)
        assertEquals(0x44, c.a)
    }

    /*
    EQUIVALENCE CLASS
     */

    @Test
    fun `NONE equals rgb(0,0,0,0)`() {
        assertEquals(ShapeColor.NONE, ShapeColor.rgb(0, 0, 0, 0))
    }
}
