package fromcrackpot.kintsugi.bfxr

/**
 * A parameter of a [BfxrSynth]: its name in a file, its default, and the values it accepts.
 *
 * - Sealed, so a `when` over it is exhaustive without an `else`.
 */
public sealed class BfxrParam {
    /** The key under `params` in a file, case-sensitive. */
    public abstract val name: String

    /** The value a sound takes when its file omits this parameter. */
    public abstract val default: BfxrValue

    /**
     * A slider: any number from [min][BfxrParam.Range.min] to [max][BfxrParam.Range.max], bounds included.
     *
     * @param name The key under `params`.
     * @param min The smallest accepted value.
     * @param max The largest accepted value.
     * @param default The value when omitted.
     */
    public class Range(
        override val name: String,
        public val min: Double,
        public val max: Double,
        override val default: BfxrValue.Number,
    ) : BfxrParam()

    /**
     * A selector: one number out of [options][BfxrParam.Choice.options].
     *
     * @param name The key under `params`.
     * @param options The accepted numbers, in the order bfxr2's editor shows them.
     * @param default The value when omitted.
     */
    public class Choice(
        override val name: String,
        public val options: List<Int>,
        override val default: BfxrValue.Number,
    ) : BfxrParam()

    /**
     * A value travelling over the sound: both ends from [min][BfxrParam.Transition.min] to
     * [max][BfxrParam.Transition.max], and a curve out of [curves][BfxrParam.Transition.curves].
     *
     * @param name The key under `params`.
     * @param min The smallest accepted end.
     * @param max The largest accepted end.
     * @param curves The accepted curve names.
     * @param default The value when omitted.
     */
    public class Transition(
        override val name: String,
        public val min: Double,
        public val max: Double,
        public val curves: List<String>,
        override val default: BfxrValue.Transition,
    ) : BfxrParam()

    /**
     * A text of at most [maxLength][BfxrParam.Text.maxLength] UTF-16 units.
     *
     * @param name The key under `params`.
     * @param maxLength The longest accepted text, in UTF-16 units.
     * @param default The value when omitted.
     */
    public class Text(
        override val name: String,
        public val maxLength: Int,
        override val default: BfxrValue.Text,
    ) : BfxrParam()
}
