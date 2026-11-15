package fromcrackpot.kintsugi.bfxr

import fromcrackpot.kintsugi.audio.Pcm
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.synths.Bfxr
import fromcrackpot.kintsugi.bfxr.synths.Birdr
import fromcrackpot.kintsugi.bfxr.synths.Boomr
import fromcrackpot.kintsugi.bfxr.synths.Bouncr
import fromcrackpot.kintsugi.bfxr.synths.Breathr
import fromcrackpot.kintsugi.bfxr.synths.Choirr
import fromcrackpot.kintsugi.bfxr.synths.Clonkr
import fromcrackpot.kintsugi.bfxr.synths.Crittr
import fromcrackpot.kintsugi.bfxr.synths.Footsteppr
import fromcrackpot.kintsugi.bfxr.synths.Fractr
import fromcrackpot.kintsugi.bfxr.synths.Glitchr
import fromcrackpot.kintsugi.bfxr.synths.Jinglr
import fromcrackpot.kintsugi.bfxr.synths.Machinr
import fromcrackpot.kintsugi.bfxr.synths.Mixr
import fromcrackpot.kintsugi.bfxr.synths.Pluckr
import fromcrackpot.kintsugi.bfxr.synths.Riftr
import fromcrackpot.kintsugi.bfxr.synths.Rustlr
import fromcrackpot.kintsugi.bfxr.synths.Signlr
import fromcrackpot.kintsugi.bfxr.synths.Squishr
import fromcrackpot.kintsugi.bfxr.synths.Swarmr
import fromcrackpot.kintsugi.bfxr.synths.Transfxr
import fromcrackpot.kintsugi.bfxr.synths.Whooshr
import fromcrackpot.kintsugi.bfxr.synths.Zappr

/**
 * A sound in the `.bfxr` format: a [BfxrSynth] and its parameter values, ready to render.
 *
 * @constructor Checks `params` against `synth`'s parameters and fills in the defaults of those omitted.
 * @param synth The synth that renders the sound.
 * @param params Values by parameter name. An omitted name takes its default.
 * @param renderSeed The seed of the noise [BfxrSynth.BFXR] and [BfxrSynth.FOOTSTEPPR] draw while rendering, in
 *   [0, 1]. The other synths seed their noise from their own parameters. A [BfxrSynth.MIXR] sound's sources use
 *   their own `renderSeed`, or its `seed`.
 * @throws BfxrFormatException when a name is unknown, a value has the wrong shape or lies outside its range,
 *   [renderSeed] lies outside [0, 1], or a [BfxrSynth.BFXR] sound would mute before its first sample.
 */
public class BfxrSound(
    public val synth: BfxrSynth,
    params: Map<String, BfxrValue> = emptyMap(),
    public val renderSeed: Double = 0.5,
) {
    /** Every parameter of [synth] by name, in the synth's order: the given values, and the defaults of the rest. */
    public val params: Map<String, BfxrValue>

    init {
        val problems = mutableListOf<String>()
        val names = synth.params.map { it.name }
        for ((name, value) in params) {
            val param = synth.params.firstOrNull { it.name == name }
            if (param == null) problems += "params.$name: unknown ${synth.id} parameter${didYouMean(name, names)}"
            else {
                problems += param.problemsWith(value, "params.$name")
                    .ifEmpty { synth.problemsWith(name, value, "params.$name") }
            }
        }
        if (!(renderSeed in 0.0..1.0)) problems += "renderSeed: $renderSeed lies outside [0, 1]"
        if (problems.isNotEmpty()) throw BfxrFormatException(problems)
        this.params = synth.params.associate { it.name to (params[it.name] ?: it.default) }
        if (synth == BfxrSynth.BFXR) bfxrMuteProblem(this.params, "params")?.let { throw BfxrFormatException(listOf(it)) }
    }

    /**
     * Renders the sound to mono PCM at 44,100 Hz, matching bfxr2's own renderer.
     *
     * - A [BfxrSynth.FOOTSTEPPR] sound can render samples past ±1, up to ±4.
     *
     * @return Fresh samples on each call.
     */
    public fun render(): Pcm = Pcm(renderSamples(synth, params, renderSeed), RATE)
}

internal fun renderSamples(synth: BfxrSynth, params: Map<String, BfxrValue>, renderSeed: Double): FloatArray =
    when (synth) {
        BfxrSynth.BFXR -> Bfxr.render(params, renderSeed)
        BfxrSynth.JINGLR -> Jinglr.render(params)
        BfxrSynth.CLONKR -> Clonkr.render(params)
        BfxrSynth.BOUNCR -> Bouncr.render(params)
        BfxrSynth.FRACTR -> Fractr.render(params)
        BfxrSynth.WHOOSHR -> Whooshr.render(params)
        BfxrSynth.TRANSFXR -> Transfxr.render(params)
        BfxrSynth.PLUCKR -> Pluckr.render(params)
        BfxrSynth.CHOIRR -> Choirr.render(params)
        BfxrSynth.BIRDR -> Birdr.render(params)
        BfxrSynth.SIGNLR -> Signlr.render(params)
        BfxrSynth.BOOMR -> Boomr.render(params)
        BfxrSynth.ZAPPR -> Zappr.render(params)
        BfxrSynth.RUSTLR -> Rustlr.render(params)
        BfxrSynth.GLITCHR -> Glitchr.render(params)
        BfxrSynth.SQUISHR -> Squishr.render(params)
        BfxrSynth.MACHINR -> Machinr.render(params)
        BfxrSynth.CRITTR -> Crittr.render(params)
        BfxrSynth.BREATHR -> Breathr.render(params)
        BfxrSynth.SWARMR -> Swarmr.render(params)
        BfxrSynth.RIFTR -> Riftr.render(params)
        BfxrSynth.MIXR -> Mixr.render(params)
        BfxrSynth.FOOTSTEPPR -> Footsteppr.render(params, renderSeed)
    }
