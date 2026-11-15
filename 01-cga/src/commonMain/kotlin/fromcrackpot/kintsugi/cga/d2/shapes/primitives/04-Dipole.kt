package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonBladeDipoleException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Two points of the plane, joined into one oriented object.
 *
 * - [start] and [end] recover the endpoints, and `start join end` rebuilds the dipole exactly.
 * - A negative [radius2] makes it imaginary. It has no real endpoints but is valid data, not an error.
 * - Orientation is data, so swapped endpoints compare unequal. Use [canonical] for unoriented equivalence.
 */
public open class Dipole : Primitive {
    /** The `e23` component: the x-component of the carrier line's unit normal, when finite. */
    public val gx: Double // e23

    /** The `e31` component: the y-component of the carrier line's unit normal, when finite. */
    public val gy: Double // e31

    /** The `e12` component: the carrier line's signed distance from the origin, when finite. */
    public val gz: Double // e12

    /**
     * The `e41` component: the x-component of the flat bulk. With [py] and [pz], it fixes where the
     * endpoints sit on the carrier.
     */
    public val px: Double // e41

    /**
     * The `e42` component: the y-component of the flat bulk. With [px] and [pz], it fixes where the
     * endpoints sit on the carrier.
     */
    public val py: Double // e42

    /** The `e43` component: minus the center's signed offset along [attitude], when finite. */
    public val pz: Double // e43

    private val _gx get() = round(gx)
    private val _gy get() = round(gy)
    private val _gz get() = round(gz)

    private val _px get() = round(px)
    private val _py get() = round(py)
    private val _pz get() = round(pz)

    /*
    CTOR
    */

    /**
     * Builds the dipole `(gx, gy, gz, px, py, pz)`, unitized, with `-0.0` cleared to `0.0`.
     *
     * - When the round weight rounds to zero but [gz][Dipole.gz] doesn't, the dipole lies at infinity and stays raw.
     *
     * @param gx The `e23` component: the carrier normal's x-component.
     * @param gy The `e31` component: the carrier normal's y-component.
     * @param gz The `e12` component: the carrier's offset term.
     * @param px The `e41` component of the flat bulk.
     * @param py The `e42` component of the flat bulk.
     * @param pz The `e43` component of the flat bulk.
     * @throws DegenerateDipoleException when the whole g-part rounds to zero at the `SCALE` quantum.
     *   The blade is then the flat point `(px, py, pz)`.
     * @throws NonBladeDipoleException when the components are not the wedge of two points.
     */
    public constructor(
        gx: Double, gy: Double, gz: Double,
        px: Double, py: Double, pz: Double,
    ) {
        val roundWeightNorm = sqrt(gx * gx + gy * gy)
        if (round(roundWeightNorm) == 0.0) {
            // Precedes the Plücker check, which a whole-zero g-part passes trivially.
            if (round(gz) == 0.0) throw DegenerateDipoleException(FlatPoint(px, py, pz))

            this.gx = gx + 0.0
            this.gy = gy + 0.0
            this.gz = gz + 0.0

            this.px = px + 0.0
            this.py = py + 0.0
            this.pz = pz + 0.0
        } else { // Unitize: d̂ = d / round_weight_norm(d)
            this.gx = gx / roundWeightNorm + 0.0
            this.gy = gy / roundWeightNorm + 0.0
            this.gz = gz / roundWeightNorm + 0.0

            this.px = px / roundWeightNorm + 0.0
            this.py = py / roundWeightNorm + 0.0
            this.pz = pz / roundWeightNorm + 0.0
        }

        val pluckerResidual = this.gx * this.px + this.gy * this.py + this.gz * this.pz
        if (round(pluckerResidual) != 0.0) throw NonBladeDipoleException(pluckerResidual)
    }

    /**
     * Adopts the components as-is, skipping unitization and the Plücker check.
     * [canonical] may use it, since negating a valid blade yields a valid blade.
     * [raw] only tells this ctor apart from the public one.
     */
    private constructor(
        gx: Double, gy: Double, gz: Double,
        px: Double, py: Double, pz: Double,
        raw: Unit
    ) {
        this.gx = gx
        this.gy = gy
        this.gz = gz

        this.px = px
        this.py = py
        this.pz = pz
    }

    /*
    PROPS
     */

    /**
     * Whether this dipole lies at infinity. True when [gx] and [gy] both round to zero at the `SCALE` quantum.
     *
     * - [center], [start] and [end] degenerate at infinity. Check this before reading them.
     */
    public val isAtInfinity: Boolean get() = _gx == 0.0 && _gy == 0.0

    /**
     * Whether this dipole is imaginary. True when [radius2] rounds below zero at the `SCALE` quantum.
     *
     * - An imaginary dipole is valid data, not an error. The meet of two disjoint circles is one.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * The raw flat bulk magnitude `√(px² + py²)`. For a finite real dipole, it is half the product of
     * the endpoints' distances from the origin.
     */
    public val norm: Double get() = sqrt(px * px + py * py) // flat_bulk_norm

    /**
     * The midpoint of the endpoints. It carries the dipole's size, so `center.radius2` equals [radius2].
     *
     * - Degenerates at infinity.
     */
    public val center: RoundPoint by cached {
        RoundPoint(
            ax = -gy * pz - gx * gz, // e1
            ay = gx * pz - gy * gz,  // e2
            az = gx * gx + gy * gy,  // e3
            aw = pz * pz - gx * py + gy * px // e4
        )
    }

    /** The raw distance of [center] from the origin, when finite. */
    public val centerNorm: Double get() = sqrt(gz * gz + pz * pz)

    /**
     * The raw, signed squared radius. For a real dipole it is the square of half the endpoint
     * distance; for an imaginary one it is negative.
     *
     * - Test imaginary-ness with [isImaginary], which rounds. This raw value is unstable near zero.
     */
    public val radius2: Double get() = pz * pz - gz * gz - 2 * (gx * py - gy * px)

    /**
     * Half the raw distance between the endpoints, never negative. For an imaginary dipole, it is the
     * magnitude of the imaginary radius.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /**
     * The first endpoint, at `center − radius·attitude`.
     *
     * - Real even for an imaginary dipole. `start join end` still rebuilds the dipole exactly.
     * - Degenerates at infinity.
     */
    public val start: RoundPoint by cached {
        RoundPoint(
            center.ax - radius * attitude.ax,
            center.ay - radius * attitude.ay,
            center.az, // attitude.az is always 0.0
            center.aw - radius * attitude.aw,
        )
    }

    /**
     * The second endpoint, at `center + radius·attitude`.
     *
     * - Real even for an imaginary dipole. `start join end` still rebuilds the dipole exactly.
     * - Degenerates at infinity.
     */
    public val end: RoundPoint by cached {
        RoundPoint(
            center.ax + radius * attitude.ax,
            center.ay + radius * attitude.ay,
            center.az, // attitude.az is always 0.0
            center.aw + radius * attitude.aw,
        )
    }

    /** The direction from [start] toward [end], as a round point at infinity. */
    public val attitude: RoundDirection by cached { RoundDirection(gy, -gx, aw = -pz) }

    /**
     * The oriented line through both endpoints.
     *
     * - It flips when the endpoints swap. Compare `carrier.canonical` for unoriented equivalence.
     */
    public val carrier: Line by cached { Line(gx, gy, gz) }

    /** The perpendicular bisector of the endpoints. Like [carrier], it flips when the endpoints swap. */
    public val cocarrier: Line by cached { Line(-gy, gx, -pz) }

    /**
     * The smallest circle containing this dipole, with the endpoints as a diameter.
     *
     * - Always inside-out. An imaginary dipole gets an imaginary container.
     *
     * @throws DegenerateCircleException when the dipole lies at infinity.
     */
    public val container: Circle by cached {
        Circle(
            cx = gx * gz + gy * pz,
            cy = gy * gz - gx * pz,
            cz = gz * gz + gx * py - gy * px,
            cw = gx * gx + gy * gy
        )
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [dipole]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param dipole The replacement geometry.
     * @return [dipole] itself. An override returns its own class rebuilt around [dipole].
     */
    public open fun copy(dipole: Dipole): Dipole = dipole

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this dipole. Both orders of the same endpoints share it.
     *
     * - Makes the first non-zero of `(gx, gy)` positive, judged on the rounded components.
     * - A dipole at infinity comes back unchanged.
     */
    public open val canonical: Dipole by cached {
        val s = if (_gx != 0.0) _gx.sign else _gy.sign
        if (s < 0) copy(
            Dipole(
                -gx + 0.0, -gy + 0.0, -gz + 0.0,
                -px + 0.0, -py + 0.0, -pz + 0.0,
                Unit
            )
        ) else this
    }

    /**
     * A fresh [Segment] wrapping this dipole. Queries against it respect the endpoints' bounds.
     *
     * - A segment never equals a dipole, since `Segment` wraps `Dipole` instead of subtyping it.
     */
    public val toSegment: Segment by cached { Segment(this) }

    /**
     * Compares the six components rounded at the `SCALE` quantum.
     *
     * - Swapped endpoints compare unequal. Compare [canonical] on both sides for unoriented equivalence.
     * - Classes must match exactly. A subclass instance never equals a plain [Dipole].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Dipole
        return _gx == other._gx
                && _gy == other._gy
                && _gz == other._gz
                && _px == other._px
                && _py == other._py
                && _pz == other._pz
    }

    private val _hashCode by cached {
        var result = _gx.hashCode()
        result = 31 * result + _gy.hashCode()
        result = 31 * result + _gz.hashCode()
        result = 31 * result + _px.hashCode()
        result = 31 * result + _py.hashCode()
        result = 31 * result + _pz.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Dipole(gx=$gx, gy=$gy, gz=$gz, px=$px, py=$py, pz=$pz)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Dipole(gx=$_gx, gy=$_gy, gz=$_gz, px=$_px, py=$_py, pz=$_pz)"
}

/**
 * `-Dipole => Dipole`<br>
 * Returns this dipole with its endpoints swapped. The points stay, the orientation flips.
 *
 * - Compares unequal to the original, but `(-dipole).canonical == dipole.canonical`.
 *
 * @receiver The dipole to negate.
 * @param T The receiver's type, preserved in the result.
 * @return The negated dipole.
 */
public operator fun <T : Dipole> T.unaryMinus(): T =
    narrowed(
        copy(
            Dipole(
                -gx, // e23
                -gy, // e31
                -gz, // e12
                -px, // e41
                -py, // e42
                -pz, // e43
            )
        )
    )
