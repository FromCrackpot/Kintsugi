package fromcrackpot.kintsugi.audio

/**
 * A mono run of PCM samples at a fixed rate: the unit a [PcmSink] loads.
 *
 * - The array is held, not copied: [PcmSink.load] takes its own copy, so later writes never reach a loaded [Clip].
 *
 * @constructor Wraps [samples] played at [sampleRate].
 * @param samples One sample per frame, nominally in [-1, 1].
 * @param sampleRate Frames per second, in Hz.
 * @throws IllegalArgumentException when [samples] is empty or [sampleRate] isn't positive.
 */
public class Pcm(
    public val samples: FloatArray,
    public val sampleRate: Int,
) {
    init {
        require(samples.isNotEmpty()) { "Pcm needs at least one sample" }
        require(sampleRate > 0) { "Pcm sample rate must be positive, got $sampleRate" }
    }

    /** The duration in seconds. */
    public val duration: Double get() = samples.size.toDouble() / sampleRate
}
