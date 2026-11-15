package fromcrackpot.kintsugi.bfxr

import fromcrackpot.kintsugi.bfxr.goldens.BFXR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.BIRDR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.BOOMR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.BOUNCR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.BREATHR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.CHOIRR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.CLONKR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.CRITTR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.FOOTSTEPPR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.FRACTR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.GLITCHR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.Golden
import fromcrackpot.kintsugi.bfxr.goldens.JINGLR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.MACHINR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.MIXR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.PLUCKR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.RIFTR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.RUSTLR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.SIGNLR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.SQUISHR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.SWARMR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.TRANSFXR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.WHOOSHR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.ZAPPR_GOLDENS
import fromcrackpot.kintsugi.bfxr.goldens.digestOf
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BfxrParityTest {
    @Test
    fun `Whooshr matches bfxr2 on every golden`() = assertParity(WHOOSHR_GOLDENS)

    @Test
    fun `Bfxr matches bfxr2 on every golden`() = assertParity(BFXR_GOLDENS)

    @Test
    fun `Jinglr matches bfxr2 on every golden`() = assertParity(JINGLR_GOLDENS)

    @Test
    fun `Clonkr matches bfxr2 on every golden`() = assertParity(CLONKR_GOLDENS)

    @Test
    fun `Bouncr matches bfxr2 on every golden`() = assertParity(BOUNCR_GOLDENS)

    @Test
    fun `Fractr matches bfxr2 on every golden`() = assertParity(FRACTR_GOLDENS)

    @Test
    fun `Transfxr matches bfxr2 on every golden`() = assertParity(TRANSFXR_GOLDENS)

    @Test
    fun `Pluckr matches bfxr2 on every golden`() = assertParity(PLUCKR_GOLDENS)

    @Test
    fun `Choirr matches bfxr2 on every golden`() = assertParity(CHOIRR_GOLDENS)

    @Test
    fun `Birdr matches bfxr2 on every golden`() = assertParity(BIRDR_GOLDENS)

    @Test
    fun `Signlr matches bfxr2 on every golden`() = assertParity(SIGNLR_GOLDENS)

    @Test
    fun `Boomr matches bfxr2 on every golden`() = assertParity(BOOMR_GOLDENS)

    @Test
    fun `Zappr matches bfxr2 on every golden`() = assertParity(ZAPPR_GOLDENS)

    @Test
    fun `Rustlr matches bfxr2 on every golden`() = assertParity(RUSTLR_GOLDENS)

    @Test
    fun `Glitchr matches bfxr2 on every golden`() = assertParity(GLITCHR_GOLDENS)

    @Test
    fun `Squishr matches bfxr2 on every golden`() = assertParity(SQUISHR_GOLDENS)

    @Test
    fun `Machinr matches bfxr2 on every golden`() = assertParity(MACHINR_GOLDENS)

    @Test
    fun `Crittr matches bfxr2 on every golden`() = assertParity(CRITTR_GOLDENS)

    @Test
    fun `Breathr matches bfxr2 on every golden`() = assertParity(BREATHR_GOLDENS)

    @Test
    fun `Swarmr matches bfxr2 on every golden`() = assertParity(SWARMR_GOLDENS)

    @Test
    fun `Riftr matches bfxr2 on every golden`() = assertParity(RIFTR_GOLDENS)

    @Test
    fun `Mixr matches bfxr2 on every golden`() = assertParity(MIXR_GOLDENS)

    @Test
    fun `Footsteppr matches bfxr2 on every golden`() = assertParity(FOOTSTEPPR_GOLDENS)

    private fun assertParity(goldens: List<Golden>) {
        var worst = 0.0
        for (golden in goldens) {
            val samples = parseBfxr(golden.sound).render().samples
            assertEquals(golden.frames, samples.size, "${golden.name}: sample count")
            val actual = digestOf(samples)
            assertEquals(golden.digest.size, actual.size, "${golden.name}: digest size")
            var error = 0.0
            var at = 0
            for (i in actual.indices) {
                val e = abs(actual[i] - golden.digest[i])
                if (e > error) {
                    error = e
                    at = i
                }
            }
            assertTrue(
                error <= TOLERANCE,
                "${golden.name}: block ${at / 3} ${DIGEST_FIELDS[at % 3]} off by $error, over the tolerance $TOLERANCE",
            )
            worst = maxOf(worst, error)
        }
        println("parity: ${goldens.size} goldens, worst error $worst")
    }

    private companion object {
        const val TOLERANCE = 1e-5
        val DIGEST_FIELDS = listOf("peak", "RMS", "first sample")
    }
}
