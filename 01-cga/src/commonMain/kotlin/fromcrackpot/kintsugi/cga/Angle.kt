package fromcrackpot.kintsugi.cga

import kotlin.jvm.JvmInline
import kotlin.math.PI

/**
 * An angle whose unit is spelled at the call site: `90.deg`, `0.8.rad`.
 *
 * - Values stay raw, with no wrapping and no `SCALE` rounding, so `370.deg > 10.deg`. [canonical] wraps.
 * - There is no trigonometry here. Read [radians] at use sites (`sin(angle.radians)`).
 *
 * @constructor Wraps the raw radian value without normalizing or validating it.
 */
@JvmInline
public value class Angle(
    /** The raw stored angle, in radians. */
    public val radians: Double,
) : Comparable<Angle> {
    /** The angle in degrees, converted from [radians] at full `Double` precision. */
    public val degrees: Double get() = radians * 180.0 / PI

    /** The equivalent angle wrapped to a single turn, in `(−180°, 180°]`: `(-270).deg.canonical == 90.deg`. */
    public val canonical: Angle get() {
        if (radians > -PI && radians <= PI) return this
        val r = radians.mod(2 * PI)
        return Angle(if (r > PI) r - 2 * PI else r)
    }

    /**
     * Compares the raw radian values of the two angles. Nothing wraps, so `370.deg > 10.deg`.
     *
     * @param other The angle to compare against.
     * @return A negative value when this angle is smaller, zero when equal, positive when larger.
     */
    override fun compareTo(other: Angle): Int = radians.compareTo(other.radians)

    /**
     * `-Angle => Angle`<br>
     * Returns the angle of the same magnitude, turning the opposite way.
     *
     * @return The negated angle.
     */
    public operator fun unaryMinus(): Angle = Angle(-radians)

    /**
     * `Angle + Angle => Angle`<br>
     * Returns the raw sum of the two angles, unwrapped.
     *
     * @param other The angle to add.
     * @return The sum.
     */
    public operator fun plus(other: Angle): Angle = Angle(radians + other.radians)

    /**
     * `Angle - Angle => Angle`<br>
     * Returns the raw difference of the two angles, unwrapped.
     *
     * @param other The angle to subtract.
     * @return The difference.
     */
    public operator fun minus(other: Angle): Angle = Angle(radians - other.radians)

    /**
     * `Angle * Double => Angle`<br>
     * Returns this angle scaled by [factor], raw.
     *
     * @param factor The scale factor.
     * @return The scaled angle.
     */
    public operator fun times(factor: Double): Angle = Angle(radians * factor)

    /**
     * `Angle / Double => Angle`<br>
     * Returns this angle divided by [divisor], raw.
     *
     * - A zero [divisor] yields an infinite angle, not an error.
     *
     * @param divisor The divisor.
     * @return The quotient.
     */
    public operator fun div(divisor: Double): Angle = Angle(radians / divisor)

    /** Returns the raw [radians] value at full stored precision. */
    override fun toString(): String = "Angle(radians=$radians)"
}

/** This value as an [Angle] in degrees: `90.deg`. */
public val Int.deg: Angle get() = Angle(this * PI / 180.0)

/** This value as an [Angle] in degrees: `22.5.deg`. */
public val Double.deg: Angle get() = Angle(this * PI / 180.0)

/** This value as an [Angle] in radians: `2.rad`. */
public val Int.rad: Angle get() = Angle(this.toDouble())

/** This value as an [Angle] in radians: `0.8.rad`. */
public val Double.rad: Angle get() = Angle(this)

/**
 * `Double * Angle => Angle`<br>
 * Returns [angle] scaled by this factor, raw.
 *
 * @receiver The scale factor.
 * @param angle The angle to scale.
 * @return The scaled angle.
 * @see Angle.times
 */
public operator fun Double.times(angle: Angle): Angle = Angle(this * angle.radians)

/**
 * Rounds [angle] to the nearest multiple of `1.0 / SCALE` radians. Use it for identity decisions,
 * never for storing.
 *
 * - Spell "do these round equal?" as `round(a - b) == 0.rad`. A rounded zero carries no sign.
 *
 * @param angle The angle to round.
 * @return The nearest multiple of `1.0 / SCALE` radians.
 */
public fun round(angle: Angle): Angle = Angle(round(angle.radians))
