package fromcrackpot.kintsugi.cga

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportTest {

    /* ROUND */

    @Test
    fun `rounds to the nearest quantum multiple`() {
        assertEquals(1e-4, round(6e-5), "above the half-quantum rounds up")
        assertEquals(0.0, round(4e-5), "below the half-quantum rounds down")
        assertEquals(-1e-4, round(-6e-5), "negative values round to the nearest multiple too")
        assertEquals(255.79, round(255.78999999999996), "FP drift lands back on the quantum multiple")
    }

    @Test
    fun `on-grid values pass through unchanged`() {
        assertEquals(42.0, round(42.0))
        assertEquals(-123.4567, round(-123.4567))
        assertEquals(1e-4, round(1e-4))
    }

    @Test
    fun `signed zero is sanitized to +0`() {
        assertEquals(0.0, round(-0.0), "-0.0 rounds to +0.0")
        assertEquals(0.0, round(-1e-13), "negative FP residue rounds to +0.0, not -0.0")
    }
}
