package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDilatorException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.round

/**
 * A dilation of the plane, applied with `shape * dilator`. It is a uniform scaling about a point,
 * or a translation.
 *
 * - Built by the [D] factory.
 * - Composes only with other dilators, since it is not a [Versor].
 * - A radius, a segment's length and a direction's length are all multiplied by the scale.
 */
public class Dilator {
    /**
     * The `e41` component: `−(1 − scale)·cy/2` of a dilation about `(cx, cy)`; `−ty/2` of a translation
     * by `(tx, ty)`.
     */
    public val dx: Double // e41

    /**
     * The `e42` component: `(1 − scale)·cx/2` of a dilation about `(cx, cy)`; `tx/2` of a translation
     * by `(tx, ty)`.
     */
    public val dy: Double // e42

    /** The `e12` component: `(1 − scale)/2` of a dilation; zero for a translation. */
    public val dz: Double // e12

    /** The `𝟙` component: `(1 + scale)/2` of a dilation; `1` for a translation. */
    public val dw: Double // 𝟙

    private val _dx get() = round(dx)
    private val _dy get() = round(dy)
    private val _dz get() = round(dz)
    private val _dw get() = round(dw)

    /*
    CTOR
     */

    /**
     * Builds the dilator `(dx, dy, dz, dw)`, normalized so that `dw + dz = 1`, with `-0.0` cleared to `0.0`.
     *
     * @param dx The `e41` component.
     * @param dy The `e42` component.
     * @param dz The `e12` component.
     * @param dw The `𝟙` component.
     * @throws DegenerateDilatorException when `dw + dz` rounds to zero, or the scale `(dw − dz)/(dw + dz)`
     *   rounds to zero or below at the `SCALE` quantum.
     */
    public constructor(dx: Double, dy: Double, dz: Double, dw: Double) {
        val norm = dw + dz
        if (round(norm) == 0.0 || round((dw - dz) / norm) <= 0.0) throw DegenerateDilatorException(dz, dw)
        if (norm == 1.0) {
            this.dx = dx + 0.0
            this.dy = dy + 0.0
            this.dz = dz + 0.0
            this.dw = dw + 0.0
        } else { // Normalize: D / (dw + dz), sign included
            this.dx = dx / norm + 0.0
            this.dy = dy / norm + 0.0
            this.dz = dz / norm + 0.0
            this.dw = dw / norm + 0.0
        }
    }

    /*
    MATH
     */

    // x ↦ scale·x + (tx, ty), read by the `* Dilator` expanded forms in the *Times.kt files.
    internal val scale get() = dw - dz
    internal val tx get() = 2.0 * dy
    internal val ty get() = -2.0 * dx

    /*
    IDENTITY
    */

    /**
     * Compares the four components rounded at the `SCALE` quantum.
     *
     * @param other The object to compare against.
     * @return `true` when [other] is a [Dilator] and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dilator) return false
        return _dx == other._dx
                && _dy == other._dy
                && _dz == other._dz
                && _dw == other._dw
    }

    private val _hashCode by cached {
        var result = _dx.hashCode()
        result = 31 * result + _dy.hashCode()
        result = 31 * result + _dz.hashCode()
        result = 31 * result + _dw.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Dilator(dx=$dx, dy=$dy, dz=$dz, dw=$dw)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Dilator(dx=$_dx, dy=$_dy, dz=$_dz, dw=$_dw)"
}

/*
FACTORY
D (dilation), single-letter like the versor factories (T/R/F/G).
 */

/**
 * Builds the dilator scaling by [scale] about [center]. Below `1` it contracts toward the center;
 * above `1` it expands from it.
 *
 * - A center at infinity degrades to a translator along it.
 *
 * @param center The dilation center, a finite flat point.
 * @param scale The positive scale factor. `1` is the identity.
 * @return The dilator.
 * @throws DegenerateDilatorException when [scale] rounds to zero or below at the `SCALE` quantum.
 */
public fun D(center: FlatPoint, scale: Double): Dilator {
    val h = (1.0 - scale) / 2.0
    val dw = (1.0 + scale) / 2.0
    if (round(scale) <= 0.0) throw DegenerateDilatorException(h * center.pz, dw)
    return Dilator(
        dx = -h * center.py, // e41
        dy = h * center.px,  // e42
        dz = h * center.pz,  // e12
        dw = dw,             // 𝟙
    )
}

/*
COMPOSITION
`next ⟇ this` expanded against the geometric antiproduct's Cayley table.
(dz, dw) multiplies as a split-complex pair (e12 ⟇ e12 = 𝟙) and e41/e42 never feed back into it,
so `dw + dz = 1` is closed: the result ctor's normalization is a no-op pass.
 */

/**
 * `Dilator * Dilator => Dilator`<br>
 * Returns the single dilator performing this dilation, then [next]'s.
 *
 * - Scales multiply. When they multiply to `1`, the composite is a translation.
 *
 * @receiver The dilator applied first.
 * @param next The dilator applied after this one.
 * @return The composite dilator.
 */
public operator fun Dilator.times(next: Dilator): Dilator = Dilator(
    dx = next.dw * this.dx + next.dx * this.dw + next.dx * this.dz - next.dz * this.dx, // e41
    dy = next.dw * this.dy + next.dy * this.dw + next.dy * this.dz - next.dz * this.dy, // e42
    dz = next.dw * this.dz + next.dz * this.dw,                                         // e12
    dw = next.dw * this.dw + next.dz * this.dz,                                         // 𝟙
)
