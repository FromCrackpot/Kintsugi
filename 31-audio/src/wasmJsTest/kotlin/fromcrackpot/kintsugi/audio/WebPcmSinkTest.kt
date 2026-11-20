@file:OptIn(ExperimentalWasmJsInterop::class)

package fromcrackpot.kintsugi.audio

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class WebPcmSinkTest {

    @Test
    fun `a browser without Web Audio fails at the factory`() {
        val saved = hideAudioContext()
        try {
            val failure = assertFailsWith<IllegalStateException> { PcmSink() }
            assertIs<JsException>(failure.cause)
        } finally {
            restoreAudioContext(saved)
        }
    }

    @Test
    fun `a failing upload throws an IllegalStateException`() {
        PcmSink().use { sink ->
            val saved = breakCreateBuffer()
            try {
                val failure = assertFailsWith<IllegalStateException> { sink.load(Pcm(floatArrayOf(0f), 44_100)) }
                assertIs<JsException>(failure.cause)
            } finally {
                restoreCreateBuffer(saved)
            }
        }
    }
}

private fun hideAudioContext(): JsAny? = js(
    "{ const d = Object.getOwnPropertyDescriptor(globalThis, 'AudioContext'); delete globalThis.AudioContext; return d; }",
)

private fun restoreAudioContext(descriptor: JsAny?): Unit =
    js("{ if (descriptor) Object.defineProperty(globalThis, 'AudioContext', descriptor); }")

private fun breakCreateBuffer(): JsAny = js(
    """{
        const original = BaseAudioContext.prototype.createBuffer;
        BaseAudioContext.prototype.createBuffer = () => { throw new DOMException('refused', 'NotSupportedError'); };
        return original;
    }""",
)

private fun restoreCreateBuffer(original: JsAny): Unit = js("{ BaseAudioContext.prototype.createBuffer = original; }")
