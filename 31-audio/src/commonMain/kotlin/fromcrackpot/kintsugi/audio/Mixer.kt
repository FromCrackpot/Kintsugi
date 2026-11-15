package fromcrackpot.kintsugi.audio

internal class Mixer {
    internal class Playing(val samples: FloatArray, val gain: Float, val loop: Boolean) {
        var position: Int = 0
            internal set
    }

    private val voices = ArrayList<Playing>()

    val active: Int get() = voices.size

    fun start(samples: FloatArray, gain: Float, loop: Boolean): Playing =
        Playing(samples, gain, loop).also { voices += it }

    fun stop(playing: Playing) {
        voices.remove(playing)
    }

    fun clear() {
        voices.clear()
    }

    fun mix(out: FloatArray) {
        out.fill(0f)
        val iterator = voices.iterator()
        while (iterator.hasNext()) {
            val voice = iterator.next()
            if (!voice.render(out)) iterator.remove()
        }
        for (i in out.indices) out[i] = out[i].coerceIn(-1f, 1f)
    }

    private fun Playing.render(out: FloatArray): Boolean {
        var frame = 0
        while (frame < out.size) {
            if (position == samples.size) {
                if (!loop) return false
                position = 0
            }
            val run = minOf(out.size - frame, samples.size - position)
            for (i in 0 until run) out[frame + i] += samples[position + i] * gain
            frame += run
            position += run
        }
        return loop || position < samples.size
    }
}
