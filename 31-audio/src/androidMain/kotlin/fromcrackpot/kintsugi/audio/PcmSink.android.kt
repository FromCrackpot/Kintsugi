package fromcrackpot.kintsugi.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public actual fun PcmSink(): PcmSink = AndroidPcmSink()

private const val BLOCK_FRAMES = 256
private const val LINE_BLOCKS = 6
private const val IDLE_SECONDS = 10
private const val IDLE_BLOCKS = IDLE_SECONDS * PcmSink.SAMPLE_RATE / BLOCK_FRAMES

private class AndroidPcmSink : PcmSink {
    private val mixer = Mixer()
    private val lock = ReentrantLock()
    private val wake = lock.newCondition()

    @Volatile
    private var track: AudioTrack = newTrack()

    @Volatile
    private var open = true
    private var dead = false
    private val pump: Thread

    init {
        pump = Thread(::pumpLoop, "kintsugi-audio").apply {
            isDaemon = true
            start()
        }
    }

    private fun pumpLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        try {
            pumpUntilClosed()
        } catch (_: Exception) {
        } finally {
            lock.withLock {
                dead = true
                mixer.clear()
            }
        }
    }

    private fun pumpUntilClosed() {
        val block = FloatArray(BLOCK_FRAMES)
        var quiet = 0
        var failed = false
        track.play()
        while (open) {
            val active = lock.withLock {
                mixer.mix(block)
                mixer.active
            }
            quiet = if (active == 0) quiet + 1 else 0
            if (quiet >= IDLE_BLOCKS) {
                idle()
                quiet = 0
                continue
            }
            if (track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING) >= 0) {
                failed = false
            } else if (!open || failed) {
                return
            } else {
                failed = true
                track.release()
                track = newTrack().apply { play() }
            }
        }
    }

    private fun idle() {
        track.pause()
        track.flush()
        lock.withLock {
            while (open && mixer.active == 0) wake.await()
        }
        if (open) track.play()
    }

    override fun load(pcm: Pcm): Clip {
        check(open) { "PcmSink is closed" }
        requirePlayable(pcm)
        val samples = pcm.samples.copyOf()
        return Clip { gain, loop ->
            check(open) { "PcmSink is closed" }
            requireGain(gain)
            lock.withLock {
                if (dead) {
                    Voice {}
                } else {
                    val playing = mixer.start(samples, gain.toFloat(), loop)
                    wake.signal()
                    Voice { lock.withLock { mixer.stop(playing) } }
                }
            }
        }
    }

    override fun close() {
        if (!open) return
        open = false
        lock.withLock { wake.signal() }
        pump.join(1_000)
        lock.withLock { mixer.clear() }
        track.release()
    }
}

private fun newTrack(): AudioTrack {
    val minimum = AudioTrack.getMinBufferSize(
        PcmSink.SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_FLOAT,
    )
    return AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(PcmSink.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(maxOf(minimum, BLOCK_FRAMES * LINE_BLOCKS * Float.SIZE_BYTES))
        .build()
}
