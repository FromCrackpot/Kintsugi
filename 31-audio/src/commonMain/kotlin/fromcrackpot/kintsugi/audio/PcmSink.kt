package fromcrackpot.kintsugi.audio

/**
 * The platform's audio output in clip mode: [load] uploads a [Pcm] once, and each [Clip.play] starts an
 * independent voice on the platform's own player.
 *
 * - Not thread-safe: call every member, and those of its clips and voices, from one thread.
 * - After [close], [load] and [Clip.play] throw; [Voice.stop] stays harmless.
 */
public interface PcmSink : AutoCloseable {
    /**
     * Uploads [pcm] to the platform player and returns the clip that plays it.
     *
     * @param pcm The samples to upload. They are copied, so the caller may reuse the array.
     * @return The loaded clip, playable until the sink closes.
     * @throws IllegalArgumentException when [pcm]'s rate isn't [SAMPLE_RATE].
     * @throws IllegalStateException when the sink is closed.
     */
    public fun load(pcm: Pcm): Clip

    /** Stops every voice and releases the audio output and every loaded clip. A second call does nothing. */
    override fun close()

    public companion object {
        /** The one sample rate every sink plays, in Hz. */
        public const val SAMPLE_RATE: Int = 44_100
    }
}

/** A [Pcm] uploaded to a [PcmSink], playable any number of times, overlapping plays included. */
public interface Clip {
    /**
     * Starts a new voice playing this clip from its first sample.
     *
     * - Past the platform's voice limit, the oldest voice is cut to make room.
     *
     * @param gain The amplitude factor, in [0, 1].
     * @param loop Whether the voice repeats until stopped.
     * @return The voice, to stop it early.
     * @throws IllegalArgumentException when [gain] lies outside [0, 1].
     * @throws IllegalStateException when the sink is closed.
     */
    public fun play(gain: Double = 1.0, loop: Boolean = false): Voice
}

/** One playback of a [Clip]. */
public fun interface Voice {
    /** Stops this voice at once. Does nothing when it already ended, was stopped, or was cut for a newer voice. */
    public fun stop()
}

/**
 * Returns the platform's [PcmSink], opening its audio output.
 *
 * - In a browser, audio stays suspended until the page's first pointer or key press: earlier plays are silent.
 * - On the JVM, a machine with no audio output fails here with the platform's line exception.
 */
public expect fun PcmSink(): PcmSink

internal fun Clip(start: (gain: Double, loop: Boolean) -> Voice): Clip = object : Clip {
    override fun play(gain: Double, loop: Boolean): Voice = start(gain, loop)
}

internal fun requirePlayable(pcm: Pcm) {
    require(pcm.sampleRate == PcmSink.SAMPLE_RATE) {
        "PcmSink plays ${PcmSink.SAMPLE_RATE} Hz PCM, got ${pcm.sampleRate} Hz"
    }
}

internal fun requireGain(gain: Double) {
    require(gain in 0.0..1.0) { "gain must lie in [0, 1], got $gain" }
}
