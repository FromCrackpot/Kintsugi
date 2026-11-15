package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.audio.Clip
import fromcrackpot.kintsugi.audio.PcmSink

/**
 * [Game]'s audio output: opens the sink at the first sound, uploads each distinct [Sound] once, and stays silent for
 * good when the sink can't open.
 */
internal class Speaker(private val open: () -> PcmSink = ::PcmSink) : AutoCloseable {
    private var sink: PcmSink? = null
    private var silent = false
    private val clips = HashMap<Sound, Clip>()

    fun play(sounds: Collection<Sound>) {
        if (sounds.isEmpty() || silent) return
        val out = sink ?: try {
            open().also { sink = it }
        } catch (_: Exception) {
            silent = true
            return
        }
        for (sound in sounds) clips.getOrPut(sound) { out.load(sound.pcm) }.play()
    }

    override fun close() {
        silent = true
        clips.clear()
        sink?.close()
        sink = null
    }
}
