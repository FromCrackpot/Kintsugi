package fromcrackpot.kintsugi.bfxr

import fromcrackpot.kintsugi.audio.PcmSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class BfxrSoundTest {
    /* SCHEMA */

    @Test
    fun `the schema covers the 23 synths with distinct ids`() {
        assertEquals(23, BfxrSynth.entries.size)
        assertEquals(23, BfxrSynth.entries.map { it.id }.toSet().size)
    }

    @Test
    fun `every synth accepts its own defaults`() {
        for (synth in BfxrSynth.entries) {
            val sound = BfxrSound(synth, synth.params.associate { it.name to it.default })
            assertEquals(synth.params.size, sound.params.size, synth.id)
        }
    }

    /* CTOR */

    @Test
    fun `omitted values take their defaults in schema order`() {
        val sound = BfxrSound(BfxrSynth.WHOOSHR, mapOf("flutter" to BfxrValue.Number(0.3)))
        assertEquals(BfxrSynth.WHOOSHR.params.map { it.name }, sound.params.keys.toList())
        assertEquals(BfxrValue.Number(0.3), sound.params["flutter"])
        assertEquals(BfxrValue.Number(0.6), sound.params["duration"])
    }

    @Test
    fun `an unknown name or a wrong shape is rejected`() {
        val e = assertFailsWith<BfxrFormatException> {
            BfxrSound(BfxrSynth.WHOOSHR, mapOf("duraton" to BfxrValue.Number(1.0), "air" to BfxrValue.Text("x")))
        }
        assertEquals(2, e.problems.size, "${e.problems}")
        assertTrue("did you mean 'duration'" in e.problems[0], e.problems[0])
    }

    @Test
    fun `the ctor checks a Jinglr phrase as parsing does`() {
        val e = assertFailsWith<BfxrFormatException> {
            BfxrSound(BfxrSynth.JINGLR, mapOf("phrase" to BfxrValue.Text("""[{"degree":20}]""")))
        }
        assertEquals(listOf("params.phrase[0].degree: 20.0 lies outside [-7, 14]"), e.problems)
    }

    @Test
    fun `a renderSeed outside the unit range is rejected`() {
        assertFailsWith<BfxrFormatException> { BfxrSound(BfxrSynth.WHOOSHR, renderSeed = -0.1) }
        assertFailsWith<BfxrFormatException> { BfxrSound(BfxrSynth.WHOOSHR, renderSeed = Double.NaN) }
    }

    /* RENDER */

    @Test
    fun `a render is the duration at the sink rate and fresh each call`() {
        val sound = BfxrSound(BfxrSynth.WHOOSHR, mapOf("duration" to BfxrValue.Number(0.25)))
        val first = sound.render()
        assertEquals(PcmSink.SAMPLE_RATE, first.sampleRate)
        assertEquals(11_025, first.samples.size)
        assertNotSame(first.samples, sound.render().samples)
    }

    @Test
    fun `every synth renders its defaults`() {
        for (synth in BfxrSynth.entries) {
            val samples = BfxrSound(synth).render().samples
            assertTrue(samples.all { it.isFinite() }, "${synth.id} renders a non-finite sample")
        }
    }
}
