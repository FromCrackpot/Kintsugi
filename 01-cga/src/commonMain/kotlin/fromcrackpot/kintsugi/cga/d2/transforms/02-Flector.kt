package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateFlectorException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.round
import kotlin.math.sqrt

/**
 * A mirror motion of the plane, applied with `shape * flector`. It is a reflection across a line,
 * or a glide reflection along one.
 *
 * - Built by the [F] (reflection) and [G] (glide reflection) factories.
 * - Reverses orientation, so a classic circle or polygon comes back inside-out.
 * - A flector and its negation apply the identical motion yet compare unequal as values.
 */
public class Flector : Versor {
    /** The `e423` component: the mirror line's unit-normal x-component. */
    public val fx: Double // e423

    /** The `e431` component: the mirror line's unit-normal y-component. */
    public val fy: Double // e431

    /** The `e412` component: the origin's signed distance from the mirror line. */
    public val fz: Double // e412

    /** The `e4` component: half the glide distance along the mirror line's running direction `(−fy, fx)`. */
    public val fw: Double // e4

    private val _fx get() = round(fx)
    private val _fy get() = round(fy)
    private val _fz get() = round(fz)
    private val _fw get() = round(fw)

    /*
    CTOR
     */

    /**
     * Builds the flector `(fx, fy, fz, fw)`, unitized to a unit mirror normal, with `-0.0` cleared to `0.0`.
     *
     * @param fx The `e423` component.
     * @param fy The `e431` component.
     * @param fz The `e412` component.
     * @param fw The `e4` component.
     * @throws DegenerateFlectorException when `√(fx² + fy²)` rounds to zero at the `SCALE` quantum,
     *   which puts the mirror at infinity.
     */
    public constructor(fx: Double, fy: Double, fz: Double, fw: Double) {
        val norm = sqrt(fx * fx + fy * fy)
        if (round(norm) == 0.0) throw DegenerateFlectorException(fz, fw)
        if (norm == 1.0) {
            this.fx = fx + 0.0
            this.fy = fy + 0.0
            this.fz = fz + 0.0
            this.fw = fw + 0.0
        } else { // Unitize: F̂ = F / flat_weight_norm(F)
            this.fx = fx / norm + 0.0
            this.fy = fy / norm + 0.0
            this.fz = fz / norm + 0.0
            this.fw = fw / norm + 0.0
        }
    }

    /*
    MATH
     */

    // Effective translation of the `* Flector` expanded forms (see the *Times.kt files).
    // The flector reflects across the line with normal (fx, fy), then translates by (tx, ty):
    //   perpendicular part → shifts the mirror (folded into fz)
    //   along-line part    → the glide (folded into fw)
    // Unitization (fx² + fy² = 1) makes the 1 − 2fx² / −2fxfy block a true reflection matrix.
    internal val tx get() = -2.0 * (fx * fz + fy * fw)
    internal val ty get() = -2.0 * (fy * fz - fx * fw)

    /*
    IDENTITY
    */

    /**
     * Compares the four components rounded at the `SCALE` quantum.
     *
     * - Compares values, not motions, so a flector and its negation compare unequal.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a [Flector] and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Flector) return false
        return _fx == other._fx
                && _fy == other._fy
                && _fz == other._fz
                && _fw == other._fw
    }

    private val _hashCode by cached {
        var result = _fx.hashCode()
        result = 31 * result + _fy.hashCode()
        result = 31 * result + _fz.hashCode()
        result = 31 * result + _fw.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Flector(fx=$fx, fy=$fy, fz=$fz, fw=$fw)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Flector(fx=$_fx, fy=$_fy, fz=$_fz, fw=$_fw)"
}

/*
FACTORY
F (reflection) and G (transflection). The single-letter convention is shared with T/R (see
Motor.kt's factory banner).
 */

/**
 * Builds the flector reflecting across [line].
 *
 * - Ignores orientation. `F(line)` and `F(-line)` apply the identical reflection.
 *
 * @param line The mirror line.
 * @return The reflection flector.
 * @throws DegenerateFlectorException when [line] lies at infinity.
 */
public fun F(line: Line): Flector = Flector(
    fx = line.gx, // e423
    fy = line.gy, // e431
    fz = line.gz, // e412
    fw = 0.0,     // e4
)

/**
 * Builds the flector reflecting across [line], then gliding [glide] units along it.
 *
 * - The glide follows [line]'s running direction, so `G(-line, d)` glides the other way.
 * - To mirror across a shifted line, shift the [Line]. There is no offset parameter.
 *
 * @param line The mirror line. Its orientation fixes the glide's positive direction.
 * @param glide The glide distance. A negative one glides the opposite way.
 * @return The glide-reflection flector.
 * @throws DegenerateFlectorException when [line] lies at infinity.
 */
public fun G(line: Line, glide: Double): Flector = Flector(
    fx = line.gx,      // e423
    fy = line.gy,      // e431
    fz = line.gz,      // e412
    fw = glide / 2.0,  // e4
)
