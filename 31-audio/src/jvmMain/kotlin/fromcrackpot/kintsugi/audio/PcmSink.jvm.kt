package fromcrackpot.kintsugi.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

public actual fun PcmSink(): PcmSink = JvmPcmSink()

private const val BLOCK_FRAMES = 256
private const val LINE_BLOCKS = 6

private class JvmPcmSink : PcmSink {
    private val mixer = Mixer()
    private val format = AudioFormat(PcmSink.SAMPLE_RATE.toFloat(), 16, 1, true, false)
    private val line: SourceDataLine = AudioSystem.getSourceDataLine(format)

    @Volatile
    private var open = true
    private val pump: Thread

    init {
        line.open(format, BLOCK_FRAMES * LINE_BLOCKS * 2)
        line.start()
        pump = Thread(::pumpLoop, "kintsugi-audio").apply {
            isDaemon = true
            start()
        }
    }

    private fun pumpLoop() {
        val block = FloatArray(BLOCK_FRAMES)
        val bytes = ByteArray(BLOCK_FRAMES * 2)
        while (open) {
            synchronized(mixer) { mixer.mix(block) }
            for (i in block.indices) {
                val sample = (block[i] * Short.MAX_VALUE).roundToInt()
                bytes[2 * i] = sample.toByte()
                bytes[2 * i + 1] = (sample shr 8).toByte()
            }
            line.write(bytes, 0, bytes.size)
        }
    }

    override fun load(pcm: Pcm): Clip {
        check(open) { "PcmSink is closed" }
        requirePlayable(pcm)
        val samples = pcm.samples.copyOf()
        return Clip { gain, loop ->
            check(open) { "PcmSink is closed" }
            requireGain(gain)
            val playing = synchronized(mixer) { mixer.start(samples, gain.toFloat(), loop) }
            Voice { synchronized(mixer) { mixer.stop(playing) } }
        }
    }

    override fun close() {
        if (!open) return
        open = false
        pump.join(1_000)
        synchronized(mixer) { mixer.clear() }
        line.stop()
        line.flush()
        line.close()
    }
}
