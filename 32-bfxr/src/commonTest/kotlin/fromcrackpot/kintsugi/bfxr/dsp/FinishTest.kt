package fromcrackpot.kintsugi.bfxr.dsp

import kotlin.math.tanh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinishTest {
    @Test
    fun `finish fades both ends over 220 samples`() {
        val out = finish(FloatArray(1_000) { if (it % 2 == 0) 0.5f else -0.5f })
        assertTrue(out[0] == 0f)
        assertEquals((tanh(0.5) * 0.5 * 0.95 * (110 / 220.0)).toFloat(), out[110])
        assertEquals((tanh(0.5) * 0.5 * 0.95).toFloat(), out[500])
        assertTrue(out[999] == 0f)
    }

    @Test
    fun `finish shapes by tanh and the clamped volume with a 0_95 margin`() {
        val buffer = FloatArray(1_000) { if (it == 500) 1f else 0f }
        val out = finish(buffer, volume = 2.0, loop = true)
        val mean = 1.0 / 1_000
        assertEquals((tanh(1.0 - mean) * 0.95).toFloat(), out[500])
        assertEquals((tanh(-mean) * 0.95).toFloat(), out[0])
    }

    @Test
    fun `finish silences samples that are not finite`() {
        val out = finish(floatArrayOf(Float.NaN, 0.5f, Float.POSITIVE_INFINITY, 0.5f), loop = true)
        assertEquals(0f, out[0])
        assertEquals(0f, out[2])
    }
}
