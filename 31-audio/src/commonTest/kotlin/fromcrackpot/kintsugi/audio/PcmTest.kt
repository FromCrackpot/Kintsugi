package fromcrackpot.kintsugi.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PcmTest {
    /* CTOR */

    @Test
    fun `empty samples are rejected`() {
        assertFailsWith<IllegalArgumentException> { Pcm(FloatArray(0), PcmSink.SAMPLE_RATE) }
    }

    @Test
    fun `a non-positive rate is rejected`() {
        assertFailsWith<IllegalArgumentException> { Pcm(FloatArray(1), 0) }
    }

    @Test
    fun `the duration is the frame count over the rate`() {
        assertEquals(0.5, Pcm(FloatArray(22_050), PcmSink.SAMPLE_RATE).duration)
    }

    /* SINK CONTRACT */

    @Test
    fun `only the sink rate is playable`() {
        requirePlayable(Pcm(FloatArray(1), PcmSink.SAMPLE_RATE))
        assertFailsWith<IllegalArgumentException> { requirePlayable(Pcm(FloatArray(1), 48_000)) }
    }

    @Test
    fun `gain must lie in the unit range`() {
        requireGain(0.0)
        requireGain(1.0)
        assertFailsWith<IllegalArgumentException> { requireGain(-0.1) }
        assertFailsWith<IllegalArgumentException> { requireGain(1.5) }
    }
}
