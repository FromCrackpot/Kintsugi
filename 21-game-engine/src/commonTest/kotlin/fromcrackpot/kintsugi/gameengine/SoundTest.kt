package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.audio.Clip
import fromcrackpot.kintsugi.audio.Pcm
import fromcrackpot.kintsugi.audio.PcmSink
import fromcrackpot.kintsugi.audio.Voice
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val BEEP = Sound(Pcm(floatArrayOf(0.1f, 0.2f), 44_100))
private val BOOP = Sound(Pcm(floatArrayOf(0.3f), 44_100))

private class SoundProbe : World(SceneBounds(-1.0, -1.0, 2.0, 2.0)) {
    var script: List<Sound> = emptyList()
    var restarting = false

    context(step: Step)
    override fun update() {
        script.forEach { it.play() }
        if (restarting) restart()
    }
}

private class FakeSink : PcmSink {
    val loads = mutableListOf<Pcm>()
    val plays = mutableListOf<Pcm>()
    var closed = false

    override fun load(pcm: Pcm): Clip {
        loads += pcm
        return object : Clip {
            override fun play(gain: Double, loop: Boolean): Voice {
                plays += pcm
                return Voice {}
            }
        }
    }

    override fun close() {
        closed = true
    }
}

class SoundTest {

    /* IDENTITY */

    @Test
    fun `equal samples make equal sounds`() {
        val twin = Sound(Pcm(floatArrayOf(0.1f, 0.2f), 44_100))
        assertEquals(BEEP, twin)
        assertEquals(BEEP.hashCode(), twin.hashCode())
        assertNotEquals(BEEP, BOOP)
    }

    @Test
    fun `a sound copies its samples`() {
        val samples = floatArrayOf(0.5f)
        val sound = Sound(Pcm(samples, 44_100))
        samples[0] = 0.9f
        assertEquals(Sound(Pcm(floatArrayOf(0.5f), 44_100)), sound)
    }

    @Test
    fun `a sound refuses any rate but the sink's`() {
        assertFailsWith<IllegalArgumentException> { Sound(Pcm(floatArrayOf(0f), 22_050)) }
    }

    /* PLAY */

    @Test
    fun `played lists the latest step's sounds in play order`() {
        val world = SoundProbe()
        assertTrue(world.played.isEmpty(), "nothing before the first step")
        world.script = listOf(BOOP, BEEP, BOOP)
        world.step(0.01)
        assertEquals(listOf(BOOP, BEEP, BOOP), world.played)
        world.script = emptyList()
        world.step(0.01)
        assertTrue(world.played.isEmpty(), "each step starts silent")
    }

    @Test
    fun `a restarting step keeps its sounds`() {
        val world = SoundProbe()
        world.script = listOf(BEEP)
        world.restarting = true
        assertFailsWith<Restart> { world.step(0.01) }
        assertEquals(listOf(BEEP), world.played)
    }

    /* SPEAKER */

    @Test
    fun `the speaker opens the sink at the first sound`() {
        var opens = 0
        val sink = FakeSink()
        val speaker = Speaker { opens++; sink }
        speaker.play(emptyList())
        assertEquals(0, opens)
        speaker.play(listOf(BEEP))
        speaker.play(listOf(BOOP))
        assertEquals(1, opens)
    }

    @Test
    fun `each distinct sound uploads once`() {
        val sink = FakeSink()
        val speaker = Speaker { sink }
        speaker.play(listOf(BEEP))
        speaker.play(listOf(Sound(Pcm(floatArrayOf(0.1f, 0.2f), 44_100)), BOOP))
        assertEquals(2, sink.loads.size)
        assertEquals(3, sink.plays.size)
    }

    @Test
    fun `a sink that fails to open leaves the speaker silent for good`() {
        var opens = 0
        val speaker = Speaker { opens++; error("no audio output") }
        speaker.play(listOf(BEEP))
        speaker.play(listOf(BEEP))
        assertEquals(1, opens)
    }

    @Test
    fun `closing the speaker closes the sink and silences it`() {
        var opens = 0
        val sink = FakeSink()
        val speaker = Speaker { opens++; sink }
        speaker.play(listOf(BEEP))
        speaker.close()
        speaker.play(listOf(BEEP))
        assertTrue(sink.closed)
        assertEquals(1, sink.plays.size)
        assertEquals(1, opens)
    }
}
