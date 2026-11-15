package fromcrackpot.kintsugi.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MixerTest {
    private fun Mixer.block(frames: Int): FloatArray = FloatArray(frames).also { mix(it) }

    @Test
    fun `an idle mixer outputs silence`() {
        assertContentEquals(FloatArray(4), Mixer().block(4))
    }

    @Test
    fun `a voice is scaled by its gain and leaves once played out`() {
        val mixer = Mixer()
        mixer.start(floatArrayOf(0.5f, -0.5f, 0.25f), 0.5f, loop = false)
        assertContentEquals(floatArrayOf(0.25f, -0.25f), mixer.block(2))
        assertEquals(1, mixer.active)
        assertContentEquals(floatArrayOf(0.125f, 0f, 0f), mixer.block(3))
        assertEquals(0, mixer.active)
    }

    @Test
    fun `a voice ending exactly on a block boundary leaves with that block`() {
        val mixer = Mixer()
        mixer.start(floatArrayOf(0.1f, 0.2f), 1f, loop = false)
        mixer.block(2)
        assertEquals(0, mixer.active)
    }

    @Test
    fun `overlapping voices sum`() {
        val mixer = Mixer()
        mixer.start(floatArrayOf(0.25f, 0.25f), 1f, loop = false)
        mixer.start(floatArrayOf(0.5f), 1f, loop = false)
        assertContentEquals(floatArrayOf(0.75f, 0.25f), mixer.block(2))
    }

    @Test
    fun `the sum is clamped to the unit range`() {
        val mixer = Mixer()
        mixer.start(floatArrayOf(0.75f, -0.75f), 1f, loop = false)
        mixer.start(floatArrayOf(0.75f, -0.75f), 1f, loop = false)
        assertContentEquals(floatArrayOf(1f, -1f), mixer.block(2))
    }

    @Test
    fun `a looping voice wraps and stays until stopped`() {
        val mixer = Mixer()
        val voice = mixer.start(floatArrayOf(0.1f, 0.2f, 0.3f), 1f, loop = true)
        assertContentEquals(floatArrayOf(0.1f, 0.2f, 0.3f, 0.1f, 0.2f), mixer.block(5))
        assertContentEquals(floatArrayOf(0.3f, 0.1f), mixer.block(2))
        assertEquals(1, mixer.active)
        mixer.stop(voice)
        assertEquals(0, mixer.active)
        assertContentEquals(FloatArray(2), mixer.block(2))
    }

    @Test
    fun `stopping a voice twice or after it ended does nothing`() {
        val mixer = Mixer()
        val voice = mixer.start(floatArrayOf(0.1f), 1f, loop = false)
        mixer.block(1)
        mixer.stop(voice)
        mixer.stop(voice)
        assertEquals(0, mixer.active)
    }
}
