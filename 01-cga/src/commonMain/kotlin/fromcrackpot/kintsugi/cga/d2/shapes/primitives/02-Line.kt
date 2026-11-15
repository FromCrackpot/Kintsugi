package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * An oriented infinite line in the plane, or a weighted line at infinity.
 *
 * - Finite lines are unitized. ([gx], [gy]) is the unit normal, and [gz] the origin's signed distance.
 * - Orientation is data, so `-line` compares unequal. Use [canonical] for unoriented equivalence.
 * - Stored raw. [equals] and [hashCode] round at the `SCALE` quantum.
 */
public open class Line : Primitive {
    /** The `e423` component: the unit normal's x-component, when finite. */
    public val gx: Double // e423

    /** The `e431` component: the unit normal's y-component, when finite. */
    public val gy: Double // e431

    /**
     * The `e412` component: the origin's signed distance when finite; the raw magnitude at
     * infinity.
     */
    public val gz: Double // e412

    private val _gx get() = round(gx)
    private val _gy get() = round(gy)
    private val _gz get() = round(gz)

    /*
    CTOR
     */

    /**
     * Builds the line `gx·x + gy·y + gz = 0`, unitized, with `-0.0` cleared to `0.0`.
     *
     * - When the normal rounds to zero at the `SCALE` quantum, the line lies at infinity and [gz] stays raw.
     *
     * @param gx The `e423` component: the normal's x-component.
     * @param gy The `e431` component: the normal's y-component.
     * @param gz The `e412` component: the offset term of `gx·x + gy·y + gz = 0`.
     */
    public constructor(gx: Double, gy: Double, gz: Double) {
        val flatWeightNorm = sqrt(gx * gx + gy * gy)

        if (round(flatWeightNorm) == 0.0) {
            this.gx = gx + 0.0
            this.gy = gy + 0.0
            this.gz = gz + 0.0
        } else { // Unitize: ĝ = g / flat_weight_norm(g)
            this.gx = gx / flatWeightNorm + 0.0
            this.gy = gy / flatWeightNorm + 0.0
            this.gz = gz / flatWeightNorm + 0.0
        }
    }

    /** Adopts the components as-is, skipping unitization. [raw] only tells this ctor apart from the public one. */
    private constructor(gx: Double, gy: Double, gz: Double, raw: Unit) {
        this.gx = gx
        this.gy = gy
        this.gz = gz
    }

    /*
    PROPS
     */

    /**
     * Whether this line lies at infinity. True when [gx] and [gy] both round to zero at the `SCALE` quantum.
     *
     * - [normal] and [attitude] degenerate to the zero direction at infinity. Check this before reading them.
     */
    public val isAtInfinity: Boolean get() = _gx == 0.0 && _gy == 0.0

    /** The raw origin distance `|gz|` for a finite line; the magnitude of a line at infinity. */
    public val norm: Double get() = abs(gz) // flat_bulk_norm

    /**
     * The unit normal as the [Direction] `(gx, gy)`. It points toward the positive side, where
     * `gx·x + gy·y + gz > 0`.
     *
     * - Degenerates at infinity.
     */
    public val normal: Direction by cached { Direction(gx, gy) }

    /**
     * The line's running direction as a free vector, 90° counter-clockwise from [normal].
     *
     * - Degenerates at infinity.
     */
    public val attitude: Direction by cached { Direction(-gy, gx) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [line]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param line The replacement geometry.
     * @return [line] itself. An override returns its own class rebuilt around [line].
     */
    public open fun copy(line: Line): Line = line

    /*
    IDENTITY
     */

    /**
     * The canonical representative of this line, the same for both orientations.
     *
     * - Makes the first non-zero of `(gx, gy)` positive, judged on the rounded components.
     * - At infinity it also ignores scale. Any non-zero line at infinity maps to `Line(0, 0, 1)`.
     */
    public open val canonical: Line by cached {
        if (_gx == 0.0 && _gy == 0.0) {
            if (_gz == 0.0) this else copy(Line(0.0, 0.0, 1.0, Unit))
        } else {
            val s = if (_gx != 0.0) _gx.sign else _gy.sign
            if (s < 0) copy(Line(-gx + 0.0, -gy + 0.0, -gz + 0.0, Unit)) else this
        }
    }

    /**
     * Compares the three components rounded at the `SCALE` quantum.
     *
     * - The negated line compares unequal. Compare [canonical] on both sides for unoriented equivalence.
     * - Classes must match exactly. A subclass instance never equals a plain [Line].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Line
        return _gx == other._gx
                && _gy == other._gy
                && _gz == other._gz
    }

    private val _hashCode by cached {
        var result = _gx.hashCode()
        result = 31 * result + _gy.hashCode()
        result = 31 * result + _gz.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Line(gx=$gx, gy=$gy, gz=$gz)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Line(gx=$_gx, gy=$_gy, gz=$_gz)"
}

/**
 * `-Line => Line`<br>
 * Returns this line with the opposite orientation. It cuts the plane in the same place, with the
 * positive side swapped.
 *
 * - Compares unequal to the original, but `(-line).canonical == line.canonical`.
 *
 * @receiver The line to negate.
 * @param T The receiver's type, preserved in the result.
 * @return The negated line.
 */
public operator fun <T : Line> T.unaryMinus(): T =
    narrowed(
        copy(
            Line(
                -gx, // e423
                -gy, // e431
                -gz, // e412
            )
        )
    )
