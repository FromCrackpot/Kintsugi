package fromcrackpot.kintsugi.cga

import kotlin.jvm.JvmInline
import kotlin.math.PI

/**
 * An angle, its unit spelled at the call site: `90.deg`, `0.8.rad`.
 *
 * The type exists so an API taking an angle never accepts a bare `Double` of ambiguous unit.
 * Deliberately no trigonometry — read [radians] at use sites (`sin(angle.radians)`).
 * Arithmetic, comparison, and equality are all raw, with no wrapping and no `SCALE` rounding —
 * an angle is a parameter, not a stored CGA component — so a sum past a full turn keeps its
 * face value and `370.deg > 10.deg` holds. [canonical] wraps a value to its single-turn
 * representative, and the module-level `round` function makes identity decisions over angles
 * just as it does over raw components.
 *
 * @constructor Wraps the raw radian value — no normalization, no validation.
 */
@JvmInline
public value class Angle(
    /** The angle in radians — the raw stored value. */
    public val radians: Double,
) : Comparable<Angle> {
    /** The angle in degrees, converted from [radians] at full `Double` precision. */
    public val degrees: Double get() = radians * 180.0 / PI

    /**
     * The equivalent angle wrapped to a single turn — the representative in `(−180°, 180°]`,
     * so `(-270).deg.canonical == 90.deg`.
     *
     * Folds the raw [radians] by whole turns into `(−π, π]`; the half turn answers `+π`
     * (`(-180).deg.canonical == 180.deg`). A value already inside the range comes back
     * unchanged — the raw fold arithmetic runs only outside it, so wrapping never perturbs an
     * in-range angle and the property is exactly idempotent. A wrapped difference of two
     * headings is the shortest signed turn between them.
     */
    public val canonical: Angle get() {
        if (radians > -PI && radians <= PI) return this
        val r = radians.mod(2 * PI)
        return Angle(if (r > PI) r - 2 * PI else r)
    }

    /**
     * Compares the two angles by their raw radian values.
     *
     * The ordering is the raw `Double` order of [radians] — no wrapping, so `370.deg > 10.deg`;
     * wrap through [canonical] first when whole turns shouldn't count. `a < b`, `a >= b`, and
     * the range form `angle in min..max` spell the comparison.
     *
     * @param other The angle to compare against.
     * @return A negative value when this angle is smaller, zero when equal, positive when
     *   larger.
     */
    override fun compareTo(other: Angle): Int = radians.compareTo(other.radians)

    /**
     * `-Angle => Angle`<br>
     * Returns the negated angle — the same magnitude turning the opposite way.
     *
     * The negation is raw and exact: only the sign of [radians] flips.
     *
     * @return The negated [Angle].
     */
    public operator fun unaryMinus(): Angle = Angle(-radians)

    /**
     * `Angle + Angle => Angle`<br>
     * Returns the sum of the two angles.
     *
     * Raw addition of the radian values — no normalization, so a sum past a full turn is kept
     * as-is.
     *
     * @param other The angle to add.
     * @return The sum [Angle].
     */
    public operator fun plus(other: Angle): Angle = Angle(radians + other.radians)

    /**
     * `Angle - Angle => Angle`<br>
     * Returns the difference of the two angles.
     *
     * Raw subtraction of the radian values — no normalization, so a difference past a half
     * turn is kept as-is.
     *
     * @param other The angle to subtract.
     * @return The difference [Angle].
     */
    public operator fun minus(other: Angle): Angle = Angle(radians - other.radians)

    /**
     * `Angle * Double => Angle`<br>
     * Returns this angle scaled by [factor].
     *
     * Raw multiplication of [radians], at full `Double` precision.
     *
     * @param factor The scale factor.
     * @return The scaled [Angle].
     */
    public operator fun times(factor: Double): Angle = Angle(radians * factor)

    /**
     * `Angle / Double => Angle`<br>
     * Returns this angle divided by [divisor].
     *
     * Raw `Double` division of [radians] — a zero [divisor] follows IEEE semantics and yields
     * an infinite angle.
     *
     * @param divisor The divisor.
     * @return The quotient [Angle].
     */
    public operator fun div(divisor: Double): Angle = Angle(radians / divisor)

    /** Returns the raw [radians] value — full stored precision, no rounding. */
    override fun toString(): String = "Angle(radians=$radians)"
}

/** This value as an [Angle] in degrees: `90.deg`. The radian conversion is raw, at full `Double` precision. */
public val Int.deg: Angle get() = Angle(this * PI / 180.0)

/** This value as an [Angle] in degrees: `22.5.deg`. The radian conversion is raw, at full `Double` precision. */
public val Double.deg: Angle get() = Angle(this * PI / 180.0)

/** This value as an [Angle] in radians: `2.rad`. */
public val Int.rad: Angle get() = Angle(this.toDouble())

/** This value as an [Angle] in radians: `0.8.rad`. */
public val Double.rad: Angle get() = Angle(this)

/**
 * `Double * Angle => Angle`<br>
 * Returns [angle] scaled by this factor.
 *
 * `Angle * Double` with the operands swapped. The swap is exact: both orders perform the same
 * raw multiplication and build the identical [Angle].
 *
 * @receiver The scale factor.
 * @param angle The angle to scale.
 * @return The scaled [Angle].
 */
public operator fun Double.times(angle: Angle): Angle = Angle(this * angle.radians)

/**
 * Rounds [angle] to the nearest multiple of the identity quantum `1.0 / SCALE` radians.
 *
 * The [Angle] form of the module's rounding helper (see [SCALE]): the quantum applies to the
 * raw [Angle.radians] value, and the result is for deciding, never for storing — "does this
 * difference round to zero?" is spelled `round(a - b) == 0.rad`. Signed zero is sanitized like
 * the `Double` form's: a rounded zero never carries a stray sign, so comparing against `0.rad`
 * is safe.
 *
 * @param angle The angle to round.
 * @return The nearest multiple of `1.0 / SCALE` radians, as an [Angle].
 */
public fun round(angle: Angle): Angle = Angle(round(angle.radians))
