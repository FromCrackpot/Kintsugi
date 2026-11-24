package fromcrackpot.kintsugi.scenegraph.style

/**
 * A stroke's dash pattern: painted and skipped runs that alternate along the stroke, in model units.
 *
 * - An odd-length list repeats once to even length, so `[2]` draws as `[2, 2]` and compares equal to it.
 * - An empty or all-zero list is [SOLID]. Its offset is dropped.
 *
 * @constructor Builds a pattern from its runs and its starting offset.
 * @throws IllegalArgumentException when a run is negative or not finite, or the offset is not finite.
 */
public class ShapeDash(
    intervals: List<Double>,
    offset: Double = 0.0,
) {
    init {
        require(intervals.all { it.isFinite() && it >= 0.0 }) {
            "dash intervals must be finite and >= 0, was $intervals"
        }
        require(offset.isFinite()) { "dash offset must be finite, was $offset" }
    }

    /** The runs, alternately painted and skipped, starting with a painted one. Even length, or empty when solid. */
    public val intervals: List<Double> = when {
        intervals.sum() == 0.0 -> emptyList()
        intervals.size % 2 == 1 -> intervals + intervals
        else -> intervals.toList()
    }

    /** How far into the pattern the stroke starts, in model units. `0.0` on [SOLID]. */
    public val offset: Double = if (this.intervals.isEmpty()) 0.0 else offset + 0.0

    /** Whether the pattern paints the whole stroke. */
    public val isSolid: Boolean get() = intervals.isEmpty()

    /**
     * Compares the normalized runs and the offset exactly.
     *
     * @param other The object to compare against.
     */
    override fun equals(other: Any?): Boolean =
        this === other || (other is ShapeDash && intervals == other.intervals && offset == other.offset)

    /** Hashes the normalized runs and the offset. */
    override fun hashCode(): Int = 31 * intervals.hashCode() + offset.hashCode()

    /** The runs and the offset, for debugging. */
    override fun toString(): String = "ShapeDash(intervals=$intervals, offset=$offset)"

    public companion object {
        /** The pattern of an unbroken stroke. Set it to keep a stroke solid where a default would dash it. */
        public val SOLID: ShapeDash = ShapeDash(emptyList())
    }
}
