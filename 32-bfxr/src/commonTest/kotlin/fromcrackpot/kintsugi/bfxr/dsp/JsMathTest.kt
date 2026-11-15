package fromcrackpot.kintsugi.bfxr.dsp

import fromcrackpot.kintsugi.bfxr.BfxrValue
import kotlin.test.Test
import kotlin.test.assertEquals

class JsMathTest {
    @Test
    fun `jsRound breaks ties toward positive infinity`() {
        assertEquals(3.0, jsRound(2.5))
        assertEquals(-2.0, jsRound(-2.5))
        assertEquals(2.0, jsRound(1.5))
        assertEquals(1.0, jsRound(0.5))
    }

    @Test
    fun `jsRound keeps the largest double below one half at zero`() {
        assertEquals(0.0, jsRound(0.49999999999999994))
    }

    @Test
    fun `jsRound keeps negative zero as JS does`() {
        assertEquals(-0.0, jsRound(-0.5))
        assertEquals(-0.0, jsRound(-0.25))
        assertEquals(-0.0, jsRound(-0.0))
    }

    @Test
    fun `toInt32 truncates toward zero`() {
        assertEquals(2, toInt32(2.9))
        assertEquals(-2, toInt32(-2.9))
        assertEquals(0, toInt32(-0.5))
    }

    @Test
    fun `toInt32 wraps modulo two to the 32 as JS does`() {
        assertEquals(Int.MIN_VALUE, toInt32(2147483648.0))
        assertEquals(Int.MAX_VALUE, toInt32(-2147483649.0))
        assertEquals(5, toInt32(4294967301.0))
        assertEquals(0, toInt32(1e300))
    }

    @Test
    fun `toInt32 maps NaN and infinities to zero`() {
        assertEquals(0, toInt32(Double.NaN))
        assertEquals(0, toInt32(Double.POSITIVE_INFINITY))
        assertEquals(0, toInt32(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `jsHypot matches V8 where a plain square root differs by an ulp`() {
        assertEquals(0.8676660048917761, jsHypot(0.7182580232620239, -0.4867748022079468))
        assertEquals(5.0, jsHypot(3.0, -4.0))
    }

    @Test
    fun `jsHypot handles zeros and infinities and NaN as JS does`() {
        assertEquals(0.0, jsHypot(0.0, -0.0))
        assertEquals(Double.POSITIVE_INFINITY, jsHypot(Double.NaN, Double.NEGATIVE_INFINITY))
        assertEquals(Double.NaN, jsHypot(Double.NaN, 1.0))
    }

    @Test
    fun `clamp passes NaN through as JS does`() {
        assertEquals(Double.NaN, clamp(Double.NaN, 0.0, 1.0))
        assertEquals(1.0, clamp(3.0, 0.0, 1.0))
        assertEquals(0.0, clamp(-3.0, 0.0, 1.0))
    }

    @Test
    fun `value falls back on a missing or non-number parameter and clamps to the given range`() {
        val params = mapOf("a" to BfxrValue.Number(9.0), "b" to BfxrValue.Text("x"))
        assertEquals(5.0, params.value("a", 0.6, 0.1, 5.0))
        assertEquals(0.4, params.value("b", 0.4))
        assertEquals(0.7, params.value("missing", 0.7))
    }
}
