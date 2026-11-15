@file:OptIn(ExperimentalWasmJsInterop::class)

package fromcrackpot.kintsugi.audio

public actual fun PcmSink(): PcmSink = WebPcmSink()

private class WebPcmSink : PcmSink {
    private var context: AudioContext? = null
    private var open = true

    private fun context(): AudioContext =
        context ?: AudioContext().also {
            context = it
            resumeOnGesture(it)
        }

    override fun load(pcm: Pcm): Clip {
        check(open) { "PcmSink is closed" }
        requirePlayable(pcm)
        val context = context()
        val samples = Float32Array(pcm.samples.size)
        pcm.samples.forEachIndexed { i, sample -> setFloat(samples, i, sample) }
        val buffer = context.createBuffer(1, pcm.samples.size, PcmSink.SAMPLE_RATE.toFloat())
        buffer.copyToChannel(samples, 0)
        return Clip { gain, loop ->
            check(open) { "PcmSink is closed" }
            requireGain(gain)
            val source = context.createBufferSource()
            source.buffer = buffer
            source.loop = loop
            val level = context.createGain()
            level.gain.value = gain.toFloat()
            source.connect(level)
            level.connect(context.destination)
            resumeIfSuspended(context)
            source.start()
            Voice { stopQuietly(source) }
        }
    }

    override fun close() {
        if (!open) return
        open = false
        context?.let(::closeQuietly)
        context = null
    }
}

private external class AudioContext : JsAny {
    val destination: AudioNode
    fun createBuffer(numberOfChannels: Int, length: Int, sampleRate: Float): AudioBuffer
    fun createBufferSource(): AudioBufferSourceNode
    fun createGain(): GainNode
}

private external interface AudioNode : JsAny {
    fun connect(destination: AudioNode): AudioNode
}

private external interface AudioBuffer : JsAny {
    fun copyToChannel(source: Float32Array, channelNumber: Int)
}

private external interface AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    var loop: Boolean
    fun start()
}

private external interface AudioParam : JsAny {
    var value: Float
}

private external interface GainNode : AudioNode {
    val gain: AudioParam
}

private external class Float32Array(length: Int) : JsAny

private fun setFloat(array: Float32Array, index: Int, value: Float): Unit = js("array[index] = value")

private fun stopQuietly(source: AudioBufferSourceNode): Unit = js("{ try { source.stop(); } catch (e) {} }")

private fun resumeIfSuspended(context: AudioContext): Unit =
    js("{ if (context.state === 'suspended') context.resume().catch(() => {}); }")

private fun closeQuietly(context: AudioContext): Unit = js("{ context.close().catch(() => {}); }")

private fun resumeOnGesture(context: AudioContext): Unit = js(
    """{
        const resume = () => { if (context.state === 'suspended') context.resume().catch(() => {}); };
        for (const type of ['pointerdown', 'keydown', 'touchend']) document.addEventListener(type, resume, true);
    }""",
)
