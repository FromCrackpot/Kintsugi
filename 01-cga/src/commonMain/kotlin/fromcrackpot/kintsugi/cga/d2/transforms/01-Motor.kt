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
 * A rigid motion of the plane — a rotation about a point, or a translation — applied to a
 * shape with `*`. Built by the [T] (translation) and [R] (rotation) factories.
 *
 * The even versor `mx·e41 + my·e42 + mz·e43 + mw·𝟙`, acting via the antiproduct sandwich
 * `M ⟇ u ⟇ M̰`, spelled `shape * motor`; [Versor] carries the right-action and composition
 * contract. Not a `Shape`: a motor carries a motion, not geometry, and appears only on the
 * right of `*`.
 *
 * One type covers both motions because the algebra makes them one family: the generator of a
 * rotation is a weighted finite flat point — the unitized flat point at `c` antisquares to
 * `−𝟙` under `⟇`, so its exponential is a CCW rotation about `c` — while an at-infinity
 * generator antisquares to `0`, making the translator the limit: a translation is a rotation
 * about a point at infinity.
 *
 * The stored form reads Euclidean: with [mz] non-zero the motor rotates counter-clockwise by
 * `θ = 2·atan2(mz, mw)` about the center `(mx/mz, my/mz)`; with [mz] zero it translates by
 * `2·mw·(my, −mx)` — [mw] is `±1` there, `+1` for a factory-built translator, `−1` for its
 * negation. Components are stored raw at full `Double` precision, unitized by the flat
 * weight norm `√(mz² + mw²)`; [equals]/[hashCode] compare rounded at the `SCALE` quantum. A
 * motor and its negation apply the identical motion yet compare unequal as values (see
 * [Versor]).
 */
public class Motor : Versor {
    /** The `e41` component — `sin(θ/2)·cx` of a rotation about `(cx, cy)`; `−dy/2` of a translation by `(dx, dy)`. */
    public val mx: Double // e41

    /** The `e42` component — `sin(θ/2)·cy` of a rotation about `(cx, cy)`; `dx/2` of a translation by `(dx, dy)`. */
    public val my: Double // e42

    /** The `e43` component — the rotation's half-angle sine `sin(θ/2)`; zero for a translation. */
    public val mz: Double // e43

    /** The `𝟙` component — the rotation's half-angle cosine `cos(θ/2)`; `±1` for a translation. */
    public val mw: Double // 𝟙

    private val _mx get() = round(mx)
    private val _my get() = round(my)
    private val _mz get() = round(mz)
    private val _mw get() = round(mw)

    /*
    CTOR
     */

    /**
     * Builds the motor `(mx, my, mz, mw)`, unitized and zero-sanitized.
     *
     * The flat weight norm `√(mz² + mw²)` divides out, so a constructed motor always stores
     * `mz² + mw² = 1` — what makes [mz] and [mw] the true half-angle sine and cosine. Signed
     * zeroes sanitize to `+0.0`. A weight norm that rounds to zero at the `SCALE` quantum
     * leaves nothing to unitize by, and no motor is built.
     *
     * @param mx The `e41` component.
     * @param my The `e42` component.
     * @param mz The `e43` component.
     * @param mw The `𝟙` component.
     * @throws DegenerateMotorException when the flat weight norm rounds to zero.
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
     * Rounded identity: equal when every component matches at the `SCALE` quantum.
     *
     * Raw components may drift within the quantum and still compare equal. Equality is by
     * value, not by motion: a motor and its negation apply the identical motion yet compare
     * unequal (see [Versor]).
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a [Motor] whose four components all match after rounding.
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

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Motor(mx=$mx, my=$my, mz=$mz, mw=$mw)"

    /** Returns the components rounded at the `SCALE` quantum — the module's debug-output convention. */
    public fun toStringRounded(): String = "Motor(mx=$_mx, my=$_my, mz=$_mz, mw=$_mw)"
}

/*
FACTORY — T (translation) and R (rotation). Single-letter names are the deliberate call-site
convention across the versor factories (T/R/F/G) — motions compose inline
(`ball * T(2.0, 3.0) * R(c, 35.deg)`)
 */

/**
 * Builds the motor translating by `(dx, dy)`.
 *
 * The translator `𝟙 + (dx/2)·e42 − (dy/2)·e41` — [R]'s at-infinity limit (see [Motor]). The
 * versor representation's half coefficients stay inside the factory: `p * T(dx, dy)` moves
 * `p` by exactly `(dx, dy)`.
 *
 * @param dx The Euclidean x-displacement.
 * @param dy The Euclidean y-displacement.
 * @return The translator [Motor].
 */
public fun T(dx: Double, dy: Double): Motor = Motor(
    mx = -dy / 2.0, // e41
    my = dx / 2.0,  // e42
    mz = 0.0,       // e43
    mw = 1.0,       // 𝟙
)

/**
 * Builds the motor translating by [direction] — a free vector's magnitude and orientation
 * taken as a displacement.
 *
 * The translator whose generator is the [Direction] itself: its `px`/`py` blades are exactly
 * the translator's, so the direction's length is the displacement length.
 *
 * @param direction The displacement.
 * @return The translator [Motor].
 */
public fun T(direction: Direction): Motor = T(direction.px, direction.py)

/**
 * Builds the motor rotating counter-clockwise by [angle] about [center].
 *
 * `cos(θ/2)·𝟙 + sin(θ/2)·(cx·e41 + cy·e42 + e43)` — the exponential of the half-angle-weighted
 * flat point at [center] (see [Motor]).
 *
 * A finite [center] never throws. An at-infinity [center] (a [Direction]) is not rejected:
 * the generator's `e43` part vanishes and the motor degrades to a translator — by
 * `2·tan(θ/2)·(dy, −dx)` after unitization, perpendicular to the direction — except at a
 * half-turn, where the weight norm collapses and no motor is built.
 *
 * @param center The rotation center, a finite [FlatPoint].
 * @param angle The rotation angle, counter-clockwise.
 * @return The rotation [Motor].
 * @throws DegenerateMotorException when [center] is at infinity and the weight norm
 *   `|cos(θ/2)|` rounds to zero — [angle] at a half-turn.
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
