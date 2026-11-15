@file:OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)

package fromcrackpot.kintsugi.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

public actual fun PcmSink(): PcmSink = IosPcmSink()

private const val MAX_PLAYERS = 32

private class IosPcmSink : PcmSink {
    private val engine = AVAudioEngine()
    private val format = AVAudioFormat(standardFormatWithSampleRate = PcmSink.SAMPLE_RATE.toDouble(), channels = 1u)
    private val players = ArrayList<Player>()
    private var open = true
    private var plays = 0L

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, null)
        session.setActive(true, null)
        engine.mainMixerNode
        engine.prepare()
        check(engine.startAndReturnError(null)) { "AVAudioEngine failed to start" }
    }

    override fun load(pcm: Pcm): Clip {
        check(open) { "PcmSink is closed" }
        requirePlayable(pcm)
        val frames = pcm.samples.size.toUInt()
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frames)
        buffer.frameLength = frames
        val channel = checkNotNull(buffer.floatChannelData?.get(0)) { "AVAudioPCMBuffer has no float channel" }
        pcm.samples.forEachIndexed { i, sample -> channel[i] = sample }
        return Clip { gain, loop ->
            check(open) { "PcmSink is closed" }
            requireGain(gain)
            player().start(buffer, gain.toFloat(), loop, ++plays)
        }
    }

    private fun player(): Player =
        players.firstOrNull { it.idle }
            ?: (if (players.size < MAX_PLAYERS) Player(newNode()).also { players += it } else null)
            ?: players.minBy { it.startedAt }

    private fun newNode(): AVAudioPlayerNode {
        val node = AVAudioPlayerNode()
        engine.attachNode(node)
        engine.connect(node, to = engine.mainMixerNode, format = format)
        return node
    }

    override fun close() {
        if (!open) return
        open = false
        players.forEach { it.node.stop() }
        players.clear()
        engine.stop()
    }
}

private class Player(val node: AVAudioPlayerNode) {
    private val playing = AtomicLong(0L)
    var startedAt = 0L
        private set

    val idle: Boolean get() = playing.load() == 0L

    fun start(buffer: AVAudioPCMBuffer, gain: Float, loop: Boolean, order: Long): Voice {
        node.stop()
        startedAt = order
        playing.store(order)
        node.volume = gain
        node.scheduleBuffer(buffer, null, if (loop) AVAudioPlayerNodeBufferLoops else 0uL) {
            playing.compareAndSet(order, 0L)
        }
        node.play()
        return Voice {
            if (playing.compareAndSet(order, 0L)) node.stop()
        }
    }
}
