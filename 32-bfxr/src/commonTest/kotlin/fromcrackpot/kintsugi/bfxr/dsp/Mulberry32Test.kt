package fromcrackpot.kintsugi.bfxr.dsp

import fromcrackpot.kintsugi.bfxr.goldens.MULBERRY32_GOLDENS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mulberry32Test {
    @Test
    fun `every golden seed draws the exact bfxr2 stream`() {
        for ((seed, draws) in MULBERRY32_GOLDENS) {
            val random = Mulberry32(seed)
            draws.forEachIndexed { i, expected -> assertEquals(expected, random.next(), "seed $seed, draw $i") }
        }
    }

    @Test
    fun `draws lie in the half-open unit range`() {
        val random = Mulberry32(0.5)
        repeat(10_000) {
            val x = random.next()
            assertTrue(x >= 0.0 && x < 1.0, "draw $x")
        }
    }
}
