package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateFlectorException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.round
import kotlin.math.sqrt

/**
 * A mirror motion of the plane — a reflection across a line, or a glide reflection along one —
 * applied to a shape with `*`. Built by the [F] (reflection) and [G] (glide reflection)
 * factories.
 *
 * The odd versor `fx·e423 + fy·e431 + fz·e412 + fw·e4`, acting via the antiproduct sandwich
 * `−F ⟇ u ⟇ F̰` — the leading minus is the odd case of the action; the spelling stays
 * `shape * flector`, and [Versor] carries the right-action and composition contract. Not a
 * `Shape`: a flector carries a motion, not geometry, and appears only on the right of `*`.
 * The improper counterpart of [Motor]: a motor's two even grades preserve orientation, a
 * flector's two odd grades — the mirror line (grade 3) and the glide (grade 1) — reverse it.
 *
 * One type covers every mirror motion because every improper isometry of the plane is a
 * transflection: a reflection across a line plus a glide along it, the pure reflection being
 * the zero-glide case. There is no blade constraint to violate — any components whose normal
 * norm doesn't round to zero build a valid flector.
 *
 * The stored form reads Euclidean: the flector reflects across the mirror line
 * `fx·x + fy·y + fz = 0`, then glides by `2·fw` along that line's running direction
 * `(−fy, fx)`. Components are stored raw at full `Double` precision, unitized by the mirror's
 * normal norm `√(fx² + fy²)`; [equals]/[hashCode] compare rounded at the `SCALE` quantum. A
 * flector and its negation apply the identical motion yet compare unequal as values (see
 * [Versor]).
 */
public class Flector : Versor {
    /** The `e423` component — the mirror line's unit-normal x-component. */
    public val fx: Double // e423

    /** The `e431` component — the mirror line's unit-normal y-component. */
    public val fy: Double // e431

    /** The `e412` component — the origin's signed distance from the mirror line. */
    public val fz: Double // e412

    /** The `e4` component — half the glide distance along the mirror line's running direction `(−fy, fx)`. */
    public val fw: Double // e4

    private val _fx get() = round(fx)
    private val _fy get() = round(fy)
    private val _fz get() = round(fz)
    private val _fw get() = round(fw)

    /*
    CTOR
     */

    /**
     * Builds the flector `(fx, fy, fz, fw)`, unitized and zero-sanitized.
     *
     * The mirror's normal norm `√(fx² + fy²)` divides out, so a constructed flector always
     * stores a unit normal — what makes [fw] exactly half the glide distance and `|fz|` the
     * mirror's distance from the origin. Signed zeroes sanitize to `+0.0`. A normal norm that
     * rounds to zero at the `SCALE` quantum describes a mirror at infinity, and no flector is
     * built.
     *
     * @param fx The `e423` component.
     * @param fy The `e431` component.
     * @param fz The `e412` component.
     * @param fw The `e4` component.
     * @throws DegenerateFlectorException when the mirror's normal norm rounds to zero.
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

    // Effective translation of the `* Flector` expanded forms (see the *Times.kt files): the flector
    // reflects across the line with normal (fx, fy), then translates by (tx, ty) — the perpendicular
    // part shifts the mirror (folded into fz), the along-line part is the glide (folded into fw).
    // Unitization (fx² + fy² = 1) makes the 1 − 2fx² / −2fxfy block a true reflection matrix.
    internal val tx get() = -2.0 * (fx * fz + fy * fw)
    internal val ty get() = -2.0 * (fy * fz - fx * fw)

    /*
    IDENTITY
    */

    /**
     * Rounded identity: equal when every component matches at the `SCALE` quantum.
     *
     * Raw components may drift within the quantum and still compare equal. Equality is by
     * value, not by motion: a flector and its negation apply the identical motion yet compare
     * unequal (see [Versor]).
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a [Flector] whose four components all match after rounding.
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

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Flector(fx=$fx, fy=$fy, fz=$fz, fw=$fw)"

    /** Returns the components rounded at the `SCALE` quantum — the module's debug-output convention. */
    public fun toStringRounded(): String = "Flector(fx=$_fx, fy=$_fy, fz=$_fz, fw=$_fw)"
}

/*
FACTORY — F (reflection) and G (transflection); the single-letter convention is shared with
T/R — see Motor.kt's factory banner.
 */

/**
 * Builds the flector reflecting across [line].
 *
 * The flector `(gx, gy, gz, 0)` — the mirror line's own components with zero glide, so the
 * reflection inherits the line's placement exactly. Reflection is orientation-blind:
 * `F(line)` and `F(-line)` build negated values applying the identical motion (see [Versor]);
 * the glide of [G] is where the line's orientation does real work. A [line] at infinity has
 * no unitized mirror form, and no flector is built.
 *
 * @param line The mirror [Line].
 * @return The reflection [Flector].
 * @throws DegenerateFlectorException when [line] is at infinity — its normal norm
 *   `√(gx² + gy²)` rounds to zero.
 */
public fun F(line: Line): Flector = Flector(
    fx = line.gx, // e423
    fy = line.gy, // e431
    fz = line.gz, // e412
    fw = 0.0,     // e4
)

/**
 * Builds the flector reflecting across [line] and gliding [glide] units along it.
 *
 * The transflection whose glide runs along the line's running direction `(−gy, gx)`, so the
 * sign of [glide] reads against the line's orientation: `G(line, d)` and `G(-line, d)`
 * reflect identically — the orientation-blindness [F] documents — but glide oppositely.
 * The versor representation's half coefficient
 * stays inside the factory: the flector stores `fw = glide/2` and the motion glides by
 * exactly [glide]. There is no perpendicular offset to give — a mirror elsewhere is spelled
 * as a shifted [Line], never as an offset here. A [line] at infinity has no unitized mirror
 * form, and no flector is built.
 *
 * @param line The mirror [Line]; its orientation fixes the glide's positive direction.
 * @param glide The glide distance along [line]'s running direction; negative glides opposite.
 * @return The glide-reflection [Flector].
 * @throws DegenerateFlectorException when [line] is at infinity — its normal norm
 *   `√(gx² + gy²)` rounds to zero.
 */
public fun G(line: Line, glide: Double): Flector = Flector(
    fx = line.gx,      // e423
    fy = line.gy,      // e431
    fz = line.gz,      // e412
    fw = glide / 2.0,  // e4
)
