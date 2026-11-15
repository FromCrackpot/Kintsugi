package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateMotorException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.round
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A rigid motion of the plane, applied with `shape * motor`. It is a rotation about a point, or a
 * translation.
 *
 * - Built by the [T] (translation) and [R] (rotation) factories.
 * - A motor and its negation apply the identical motion yet compare unequal as values.
 */
public class Motor : Versor {
    /**
     * The `e41` component: `sin(θ/2)·cx` of a rotation about `(cx, cy)`; `−dy/2` of a translation by
     * `(dx, dy)`.
     */
    public val mx: Double // e41

    /** The `e42` component: `sin(θ/2)·cy` of a rotation about `(cx, cy)`; `dx/2` of a translation by `(dx, dy)`. */
    public val my: Double // e42

    /** The `e43` component: the rotation's half-angle sine `sin(θ/2)`; zero for a translation. */
    public val mz: Double // e43

    /** The `𝟙` component: the rotation's half-angle cosine `cos(θ/2)`; `±1` for a translation. */
    public val mw: Double // 𝟙

    private val _mx get() = round(mx)
    private val _my get() = round(my)
    private val _mz get() = round(mz)
    private val _mw get() = round(mw)

    /*
    CTOR
     */

    /**
     * Builds the motor `(mx, my, mz, mw)`, unitized so that `mz² + mw² = 1`, with `-0.0` cleared to `0.0`.
     *
     * @param mx The `e41` component.
     * @param my The `e42` component.
     * @param mz The `e43` component.
     * @param mw The `𝟙` component.
     * @throws DegenerateMotorException when `√(mz² + mw²)` rounds to zero at the `SCALE` quantum.
     */
    public constructor(mx: Double, my: Double, mz: Double, mw: Double) {
        val norm = sqrt(mz * mz + mw * mw)
        if (round(norm) == 0.0) throw DegenerateMotorException(mx, my)
        if (norm == 1.0) {
            this.mx = mx + 0.0
            this.my = my + 0.0
            this.mz = mz + 0.0
            this.mw = mw + 0.0
        } else { // Unitize: M̂ = M / flat_weight_norm(M)
            this.mx = mx / norm + 0.0
            this.my = my / norm + 0.0
            this.mz = mz / norm + 0.0
            this.mw = mw / norm + 0.0
        }
    }

    /*
    MATH
     */

    // Rigid-motion decomposition shared by the `* Motor` expanded forms in the *Times.kt files:
    // a motor is a rotation R plus a residual translation t when the center is off-origin.
    // Unitization (mz² + mw² = 1) makes cos/sin the true cosine/sine of the rotation angle.
    internal val cos get() = mw * mw - mz * mz
    internal val sin get() = 2.0 * mw * mz
    internal val tx get() = 2.0 * (mw * my + mx * mz)
    internal val ty get() = 2.0 * (my * mz - mw * mx)

    /*
    IDENTITY
    */

    /**
     * Compares the four components rounded at the `SCALE` quantum.
     *
     * - Compares values, not motions, so a motor and its negation compare unequal.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a [Motor] and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Motor) return false
        return _mx == other._mx
                && _my == other._my
                && _mz == other._mz
                && _mw == other._mw
    }

    private val _hashCode by cached {
        var result = _mx.hashCode()
        result = 31 * result + _my.hashCode()
        result = 31 * result + _mz.hashCode()
        result = 31 * result + _mw.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Motor(mx=$mx, my=$my, mz=$mz, mw=$mw)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Motor(mx=$_mx, my=$_my, mz=$_mz, mw=$_mw)"
}

/*
FACTORY
T (translation) and R (rotation). Single-letter names are the deliberate call-site convention of
the versor factories (T/R/F/G), so motions compose inline: `ball * T(2.0, 3.0) * R(c, 35.deg)`.
 */

/**
 * Builds the motor translating by exactly `(dx, dy)`.
 *
 * @param dx The Euclidean x-displacement.
 * @param dy The Euclidean y-displacement.
 * @return The translator.
 */
public fun T(dx: Double, dy: Double): Motor = Motor(
    mx = -dy / 2.0, // e41
    my = dx / 2.0,  // e42
    mz = 0.0,       // e43
    mw = 1.0,       // 𝟙
)

/**
 * Builds the motor translating by [direction]. Its length is the distance, and its orientation the
 * heading.
 *
 * @param direction The displacement.
 * @return The translator.
 */
public fun T(direction: Direction): Motor = T(direction.px, direction.py)

/**
 * Builds the motor rotating counter-clockwise by [angle] about [center].
 *
 * - A center at infinity degrades to a translator perpendicular to it.
 *
 * @param center The rotation center, a finite flat point.
 * @param angle The counter-clockwise rotation angle.
 * @return The rotation motor.
 * @throws DegenerateMotorException when [center] lies at infinity and [angle] is a half-turn.
 */
public fun R(center: FlatPoint, angle: Angle): Motor {
    val half = angle.radians / 2.0
    val s = sin(half)
    return Motor(
        mx = s * center.px, // e41
        my = s * center.py, // e42
        mz = s * center.pz, // e43
        mw = cos(half),     // 𝟙
    )
}
