package fromcrackpot.kintsugi.bfxr

/**
 * A parameter value of a [BfxrSound], in one of the three shapes the `.bfxr` format stores.
 *
 * - Sealed, so a `when` over it is exhaustive without an `else`.
 */
public sealed interface BfxrValue {
    /**
     * A plain number: a slider position, or the option a [BfxrParam.Choice] selects.
     *
     * @param value The number.
     */
    public data class Number(public val value: Double) : BfxrValue

    /**
     * A value travelling from [start] to [end] over the sound, along [curve].
     *
     * @param start The value at the sound's start.
     * @param end The value at the sound's end.
     * @param curve The name of the easing curve between them.
     */
    public data class Transition(public val start: Double, public val end: Double, public val curve: String) : BfxrValue

    /**
     * A text value, such as a melody or a nested sound encoded as JSON.
     *
     * @param value The text.
     */
    public data class Text(public val value: String) : BfxrValue
}
