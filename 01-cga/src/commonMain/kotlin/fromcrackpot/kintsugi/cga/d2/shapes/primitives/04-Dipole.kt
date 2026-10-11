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
 * Two points of the plane joined into a single oriented object — the 2D dipole.
 *
 * The grade-2 blade `gx·e23 + gy·e31 + gz·e12 + px·e41 + py·e42 + pz·e43`, the wedge of two
 * round points. The constructor unitizes by the round weight `‖(gx, gy)‖` — the distance
 * between the endpoints — and rejects component sets violating the Plücker blade constraint.
 * The endpoints are recovered as [start] and [end], with the line through them, their midpoint
 * and their half-distance as first-class views ([carrier], [center], [radius]); the wedge
 * rebuilds the blade: `start ∧ end == this`. A negative [radius2] makes the dipole imaginary —
 * no real endpoints, first-class data, never an error (see [isImaginary]). Orientation is data:
 * swapping the endpoints negates every component and [equals] distinguishes them ([canonical]
 * for unoriented equivalence). A round weight that rounds to zero leaves the dipole at infinity,
 * all six components raw ([isAtInfinity]). Components are stored raw at full `Double`
 * precision; [equals]/[hashCode] compare rounded at the `SCALE` quantum.
 */
public open class Dipole : Primitive {
    /** The `e23` component — the carrier line's unit-normal x-component when finite. */
    public val gx: Double // e23

    /** The `e31` component — the carrier line's unit-normal y-component when finite. */
    public val gy: Double // e31

    /** The `e12` component — the carrier line's signed origin distance when finite. */
    public val gz: Double // e12

    /**
     * The `e41` component — the flat bulk's x-component; with [py] and [pz] it fixes the
     * endpoints' placement along the carrier.
     */
    public val px: Double // e41

    /**
     * The `e42` component — the flat bulk's y-component; with [px] and [pz] it fixes the
     * endpoints' placement along the carrier.
     */
    public val py: Double // e42

    /** The `e43` component — minus the center's signed offset along [attitude] when finite. */
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
     * Builds the dipole `(gx, gy, gz, px, py, pz)`, unitized, zero-sanitized, and blade-checked.
     *
     * A round weight `‖(gx, gy)‖` that doesn't round to zero is divided out of every component,
     * leaving the g-part the carrier line's unit normal and offset. A weight that rounds to zero
     * at the `SCALE` quantum splits on [gz]: non-zero leaves the dipole at infinity with all six
     * components raw (at-infinity weights are data — no unitization, no canonicalization), while
     * a whole g-part of zero means the blade lies in the `e41/e42/e43` span — it is the flat
     * point `(px, py, pz)`, not a dipole, and the constructor throws. Accepted components must
     * satisfy the Plücker blade constraint `gx·px + gy·py + gz·pz = 0` — what separates a
     * factorizable wedge of two points from a general bivector; the residual is evaluated on the
     * unitized components and must round to zero. Signed zeroes sanitize to `+0.0`.
     *
     * @param gx The `e23` component: the carrier normal's x-component.
     * @param gy The `e31` component: the carrier normal's y-component.
     * @param gz The `e12` component: the carrier's offset term.
     * @param px The `e41` component of the flat bulk.
     * @param py The `e42` component of the flat bulk.
     * @param pz The `e43` component of the flat bulk.
     * @throws DegenerateDipoleException When the whole g-part rounds to zero — the blade
     *   collapses to the payload `FlatPoint(px, py, pz)`.
     * @throws NonBladeDipoleException When the Plücker residual doesn't round to zero; carries
     *   the residual for diagnostics.
     */
    public constructor(
        gx: Double, gy: Double, gz: Double,
        px: Double, py: Double, pz: Double,
    ) {
        val roundWeightNorm = sqrt(gx * gx + gy * gy)
        if (round(roundWeightNorm) == 0.0) {
            // Must precede the Plücker check — a whole-zero g-part passes the residual trivially.
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
     * Adopts components as-is, skipping unitization and the Plücker check — sound for
     * [canonical], whose sign-negation of a valid blade is itself valid; [raw] disambiguates
     * from the public ctor.
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
     * Whether this dipole lies at infinity — a purely directional object rather than two points
     * in the plane.
     *
     * `true` when [gx] and [gy] both round to zero at the `SCALE` quantum; the remaining
     * components are raw data. The derived points [center], [start] and [end] degenerate at
     * infinity and the wedge identity `start ∧ end == this` doesn't apply — gate on this
     * predicate before reading them.
     */
    public val isAtInfinity: Boolean get() = _gx == 0.0 && _gy == 0.0

    /**
     * Whether this dipole is imaginary — a negative squared radius, no real endpoints.
     *
     * `true` when [radius2] rounds below zero at the `SCALE` quantum. Imaginary dipoles are
     * first-class data (an imaginary dipole can encode two separated circles' distance),
     * never an error.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * The flat bulk magnitude `√(px² + py²)` — raw.
     *
     * The flat bulk norm: for a finite real dipole, half the product of the endpoints'
     * distances from the origin.
     */
    public val norm: Double get() = sqrt(px * px + py * py) // flat_bulk_norm

    /**
     * The midpoint of the endpoints, carrying the dipole's size — a [RoundPoint].
     *
     * The center's `aw` sits `radius2/2` above the null lift, so `center.radius2` equals this
     * dipole's [radius2] — exactly what lets [start] and [end] fall out componentwise as
     * `center ∓ radius·attitude`. Finite dipoles yield a unitized center (`az = 1`; read the
     * coordinates off `ax`/`ay`); at infinity the center degenerates.
     */
    public val center: RoundPoint by cached {
        RoundPoint(
            ax = -gy * pz - gx * gz, // e1
            ay = gx * pz - gy * gz,  // e2
            az = gx * gx + gy * gy,  // e3
            aw = pz * pz - gx * py + gy * px // e4
        )
    }

    /**
     * The center's distance from the origin `√(gz² + pz²)` — raw.
     *
     * The center norm: the componentwise shortcut for the distance of [center] from the
     * origin when finite.
     */
    public val centerNorm: Double get() = sqrt(gz * gz + pz * pz)

    /**
     * The squared radius — raw and signed; negative for an imaginary dipole.
     *
     * `pz² − gz² − 2·(gx·py − gy·px)`: the square of half the distance between the endpoints
     * for a real dipole, exactly zero when they coincide — the limit of the endpoints merging
     * into one point — and negative when no real endpoints exist. To test imaginary-ness use
     * [isImaginary], which rounds; comparing this raw value to zero is FP-fragile.
     */
    public val radius2: Double get() = pz * pz - gz * gz - 2 * (gx * py - gy * px)

    /**
     * The radius magnitude `√(|radius2|)` — raw, non-negative.
     *
     * Half the distance between the endpoints for a real dipole. For an imaginary dipole this
     * is the magnitude of the imaginary radius — gate on [isImaginary] where the distinction
     * matters.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /**
     * The first endpoint — `center − radius·attitude`.
     *
     * Paired with [end], it recovers the blade: the wedge `start ∧ end == this` holds exactly,
     * for imaginary dipoles too — an imaginary dipole yields real-valued endpoints whose wedge
     * rebuilds the imaginary blade. Degenerates at infinity ([isAtInfinity]).
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
     * The second endpoint — `center + radius·attitude`.
     *
     * Paired with [start], it recovers the blade: the wedge `start ∧ end == this` holds exactly,
     * for imaginary dipoles too — an imaginary dipole yields real-valued endpoints whose wedge
     * rebuilds the imaginary blade. Degenerates at infinity ([isAtInfinity]).
     */
    public val end: RoundPoint by cached {
        RoundPoint(
            center.ax + radius * attitude.ax,
            center.ay + radius * attitude.ay,
            center.az, // attitude.az is always 0.0
            center.aw + radius * attitude.aw,
        )
    }

    /**
     * The direction from [start] toward [end] — a round point at infinity.
     *
     * The attitude `att(d) = d ∨ e412`, one antigrade up: the [RoundDirection]
     * `(gy, −gx, 0, −pz)`, a purely directional object with `radius2 = −1` when finite.
     */
    public val attitude: RoundDirection by cached { RoundDirection(gy, -gx, aw = -pz) }

    /**
     * The line through both points — the carrier `car(d) = e4 ∧ d`.
     *
     * Componentwise the g-part itself: the [Line] `(gx, gy, gz)`. Orientation-sensitive — the
     * g-part flips with the wedge order, so carriers of the same point set built from opposite
     * orderings compare unequal; compare `carrier.canonical` for unoriented equivalence.
     */
    public val carrier: Line by cached { Line(gx, gy, gz) }

    /**
     * The co-carrier — the perpendicular bisector of the endpoints.
     *
     * `ccr(d) = e4 ∧ d^☆`: the [Line] `(−gy, gx, −pz)`, whose normal runs opposite [attitude].
     * Meets [carrier] at the dipole's center, and is orientation-sensitive like it.
     */
    public val cocarrier: Line by cached { Line(-gy, gx, -pz) }

    /**
     * The smallest circle containing this dipole — the endpoints as a diameter.
     *
     * `con(d) = d ∧ car(d)^☆`, always inside-out (`cw = +1` — what the algebra gives);
     * preserves the size: `container.radius2 == radius2`, so an imaginary dipole gets an
     * imaginary container. At infinity the round weight `cw = gx² + gy²` collapses to zero,
     * and no container exists.
     *
     * @throws DegenerateCircleException At infinity — the collapsed round weight leaves the
     *   [Circle] constructor nothing to unitize by.
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
     * Returns this object rebuilt around the replacement geometry [dipole] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a dipole computes its result as a plain [Dipole] and returns
     * it through this hook. This base implementation returns [dipole] itself; a subclass
     * overrides it to rebuild an instance of its own class around [dipole], and every such
     * operation then comes back as that class. The result's runtime class must equal the
     * receiver's — the operations check it and fail with a named error when a subclass misses
     * the override.
     *
     * @param dipole The replacement geometry.
     * @return [dipole] itself; an override returns its own class rebuilt around [dipole].
     */
    public open fun copy(dipole: Dipole): Dipole = dipole

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this dipole — useful for unoriented equivalence.
     *
     * `a.canonical == b.canonical` detects the same endpoints under opposed wedge orders. The
     * canonical orientation makes the first non-zero of `(gx, gy)` positive, negating all six
     * components when it flips; the sign decision reads rounded components, so `a == b` implies
     * `a.canonical == b.canonical`. At infinity the rounded g-part is zero and the dipole is
     * returned unchanged (at-infinity weights are data). The representative is rebuilt through
     * [copy], so a subclass comes back as its own class; an already-canonical dipole returns
     * `this`.
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
     * A fresh [Segment] wrapping this dipole — the promotion across the algebra/composite
     * boundary.
     *
     * Selecting `Segment` makes geometric answers respect the chord's bounds instead of the
     * whole carrier. The result is `!= this` — `Segment` wraps rather than subtypes `Dipole`.
     */
    public val toSegment: Segment by cached { Segment(this) }

    /**
     * Rounded, orientation-sensitive identity: equal when every component matches at the
     * `SCALE` quantum.
     *
     * Swapping the endpoints negates all six components and compares unequal; compare
     * [canonical] on both sides for unoriented equivalence. At-infinity magnitudes compare
     * as data. Identity is exact-class: a subclass instance and a plain [Dipole] never
     * compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its six components
     *   all match after rounding.
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

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Dipole(gx=$gx, gy=$gy, gz=$gz, px=$px, py=$py, pz=$pz)"

    /** Returns the components rounded at the `SCALE` quantum — the debug-output convention of every primitive. */
    public fun toStringRounded(): String = "Dipole(gx=$_gx, gy=$_gy, gz=$_gz, px=$_px, py=$_py, pz=$_pz)"
}

/**
 * `-Dipole => Dipole`<br>
 * Returns this dipole with its endpoints exchanged — the same point pair, opposite orientation.
 *
 * Negates all six components, raw at full `Double` precision — exactly what swapping the wedge
 * order does, so [Dipole.start] and [Dipole.end] exchange while [Dipole.center] and
 * [Dipole.radius2] are untouched; [Dipole.attitude] flips. A dipole at infinity negates its
 * raw components, which are data. Identity is orientation-sensitive, so a dipole and its
 * negation compare unequal; compare [Dipole.canonical] for unoriented equivalence —
 * `(-dipole).canonical == dipole.canonical`. The result is the receiver's own class: the
 * negated geometry returns through [Dipole.copy], preserving a subclass that overrides the
 * hook.
 *
 * @receiver The [Dipole] to negate.
 * @param T The receiver's type, preserved in the result.
 * @return The negated dipole, as the receiver's class.
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
