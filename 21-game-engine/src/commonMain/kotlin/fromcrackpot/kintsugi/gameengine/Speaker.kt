package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.audio.Clip
import fromcrackpot.kintsugi.audio.PcmSink

/**
 * [Game]'s audio output: opens the sink at the first sound, uploads each distinct [Sound] once, and closes the sink
 * and stays silent for good at the sink's first failure.
 */
internal class Speaker(private val open: () -> PcmSink = ::PcmSink) : AutoCloseable {
    private var sink: PcmSink? = null
    private var silent = false
    private val clips = HashMap<Sound, Clip>()

    fun play(sounds: Collection<Sound>) {
        if (sounds.isEmpty() || silent) return
        try {
            val out = sink ?: open().also { sink = it }
            for (sound in sounds) clips.getOrPut(sound) { out.load(sound.pcm) }.play()
        } catch (_: Exception) {
            close()
        }
    }

    override fun close() {
        silent = true
        clips.clear()
        sink?.close()
        sink = null
    }
}
