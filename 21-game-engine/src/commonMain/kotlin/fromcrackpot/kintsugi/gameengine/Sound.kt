package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.audio.Pcm
import fromcrackpot.kintsugi.audio.PcmSink

/**
 * A sound a world plays: mono samples at 44,100 Hz, compared by content.
 *
 * - Equal sounds share one upload to the audio output, so a sound loaded again for each new world costs nothing new.
 *
 * @constructor Copies [pcm]'s samples, so later writes to its array never reach the sound.
 * @param pcm The samples to play.
 * @throws IllegalArgumentException when [pcm]'s rate isn't [PcmSink.SAMPLE_RATE].
 */
public class Sound(pcm: Pcm) {
    internal val pcm: Pcm

    private val hash: Int

    init {
        require(pcm.sampleRate == PcmSink.SAMPLE_RATE) {
            "a Sound plays ${PcmSink.SAMPLE_RATE} Hz PCM, got ${pcm.sampleRate} Hz"
        }
        this.pcm = Pcm(pcm.samples.copyOf(), pcm.sampleRate)
        hash = this.pcm.samples.contentHashCode()
    }

    /**
     * Compares the samples bit for bit.
     *
     * @param other The object to compare against.
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is Sound && hash == other.hash && pcm.samples.contentEquals(other.pcm.samples)

    /** Hashes the samples. */
    override fun hashCode(): Int = hash

    /** Names the sample count. */
    override fun toString(): String = "Sound(${pcm.samples.size} samples)"
}

/**
 * Plays this sound once the frame's steps are done; [World.played] lists it until the next step.
 *
 * - Callable only while a [Step] is underway, inside a [World].
 * - Equal sounds played within one frame start once. A voice plays to its end, past a [restart] too.
 */
context(world: World, _: Step)
public fun Sound.play() {
    world.record(this)
}
