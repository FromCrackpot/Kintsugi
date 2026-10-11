package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteRoundDirectionException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.round
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * A point in the plane, optionally carrying a size — the 2D round point.
 *
 * The grade-1 vector `ax·e1 + ay·e2 + az·e3 + aw·e4`. The constructor unitizes by the round
 * weight [az], so a finite point stores its Euclidean coordinates in ([ax], [ay]). [aw] is the
 * conformal component; measured against the null lift `aw = (ax² + ay²)/2` it gives the squared
 * radius [radius2] — zero for a plain Euclidean point, negative for an imaginary one (see
 * [isImaginary]). A weight that rounds to zero places the point at infinity: a direction with
 * magnitude, kept raw. Components are stored raw at full `Double` precision;
 * [equals]/[hashCode] compare rounded at the `SCALE` quantum.
 */
public open class RoundPoint : Primitive {
    /** The `e1` component — the Euclidean x-coordinate when finite. */
    public val ax: Double // e1

    /** The `e2` component — the Euclidean y-coordinate when finite. */
    public val ay: Double // e2

    /** The `e3` round weight — `1` when finite; rounds to `0` at infinity (kept raw). */
    public val az: Double // e3

    /** The `e4` conformal component — `(ax² + ay²)/2` for a plain Euclidean point. */
    public val aw: Double // e4

    private val _ax get() = round(ax)
    private val _ay get() = round(ay)
    private val _az get() = round(az)
    private val _aw get() = round(aw)

    /*
    CTOR
     */

    /**
     * Builds the round point `(ax, ay, az, aw)`, unitized and zero-sanitized.
     *
     * A round weight [az] that doesn't round to zero is divided out of every component. A
     * weight that rounds to zero at the `SCALE` quantum leaves the point at infinity, all
     * four components kept raw: the magnitude is data. Signed zeroes sanitize to `+0.0`.
     *
     * @param ax The `e1` component.
     * @param ay The `e2` component.
     * @param az The `e3` round weight; `0` places the point at infinity.
     * @param aw The `e4` conformal component; `(ax² + ay²)/2` makes a plain Euclidean point.
     */
    public constructor(ax: Double, ay: Double, az: Double, aw: Double) {
        if (round(az) == 0.0 || az == 1.0) {
            this.ax = ax + 0.0
            this.ay = ay + 0.0
            this.az = az + 0.0
            this.aw = aw + 0.0
        } else { // Unitize: â = a / round_weight_norm(a)
            this.ax = ax / az + 0.0
            this.ay = ay / az + 0.0
            this.az = 1.0
            this.aw = aw / az + 0.0
        }
    }

    /**
     * Builds the Euclidean point `(ax, ay)` with its conformal lift.
     *
     * The null lift `aw = (ax² + ay²)/2` makes [radius2] exactly zero — a plain point with no
     * size.
     *
     * @param ax The Euclidean x-coordinate.
     * @param ay The Euclidean y-coordinate.
     */
    public constructor(ax: Double, ay: Double) : this(ax, ay, az = 1.0, aw = (ax * ax + ay * ay) / 2.0)

    /*
    PROPS
     */

    /**
     * Whether this point lies at infinity — a direction with magnitude rather than a position.
     *
     * `true` when [az] rounds to zero at the `SCALE` quantum.
     */
    public val isAtInfinity: Boolean get() = _az == 0.0

    /**
     * Whether this point is imaginary — a negative squared radius, no real locus.
     *
     * `true` when [radius2] rounds below zero at the `SCALE` quantum. Imaginary round points
     * are first-class data (a computed point can encode two shapes' separation as an imaginary
     * radius), never an error.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * The Euclidean magnitude `√(ax² + ay²)` — raw.
     *
     * The round bulk norm (also the center norm): the distance from the origin for a
     * unitized finite point, the free magnitude at infinity.
     */
    public val norm: Double get() = sqrt(ax * ax + ay * ay) // round_bulk_norm / center_norm

    /**
     * The squared radius — raw and signed; negative for an imaginary point.
     *
     * `2·az·aw − ax² − ay²`: exactly zero for a plain Euclidean point (the null lift),
     * non-zero for sized points — e.g. a [Circle.center] carries its circle's squared radius.
     * To test imaginary-ness use [isImaginary], which rounds; comparing this raw value to
     * zero is FP-fragile.
     */
    public val radius2: Double get() = 2 * az * aw - ax * ax - ay * ay + 0.0

    /**
     * The radius magnitude `√(|radius2|)` — raw, non-negative.
     *
     * For an imaginary point this is the magnitude of the imaginary radius — gate on
     * [isImaginary] where the distinction matters.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /**
     * The attitude, degenerate for a point — pure weight, no direction.
     *
     * `att(a) = a ∨ e412`, one antigrade up; scalar-valued for a round point, hence a plain
     * `Double`: [az].
     */
    public val attitude: Double get() = az // Scalar

    /**
     * The flat point at this position — the carrier `car(a) = e4 ∧ a`.
     *
     * Component-identical to [toFlat]; this member keeps the algebraic name for derivations.
     */
    public val carrier: FlatPoint by cached { FlatPoint(ax, ay, az) }

    /**
     * The co-carrier, degenerate for a point — just its weight [az].
     *
     * `ccr(a) = e4 ∧ a^☆` is antiscalar-valued for a round point, and antiscalar results are
     * plain `Double`s.
     */
    public val cocarrier: Double get() = az // AntiScalar

    /**
     * The smallest circle containing this point — zero radius for a plain Euclidean point.
     *
     * `con(a) = a ∧ car(a)^☆`, always inside-out (`cw = +1` — what the algebra gives);
     * `container.radius2 == az²·radius2`, so an imaginary point gets an imaginary container.
     * At infinity the round weight `cw = az²` collapses to zero, and no container exists.
     *
     * @throws DegenerateCircleException At infinity — the collapsed round weight leaves the
     *   [Circle] constructor nothing to unitize by.
     */
    public val container: Circle by cached {
        Circle(
            cx = -ax * az,
            cy = -ay * az,
            cz = ax * ax + ay * ay - az * aw,
            cw = az * az
        )
    }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [roundPoint] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a round point computes its result as a plain [RoundPoint] and
     * returns it through this hook. This base implementation returns [roundPoint] itself; a
     * subclass overrides it to rebuild an instance of its own class around [roundPoint], and
     * every such operation then comes back as that class. The result's runtime class must
     * equal the receiver's — the operations check it and fail with a named error when a
     * subclass misses the override.
     *
     * @param roundPoint The replacement geometry.
     * @return [roundPoint] itself; an override returns its own class rebuilt around [roundPoint].
     */
    public open fun copy(roundPoint: RoundPoint): RoundPoint = roundPoint

    /*
    IDENTITY
    */

    /**
     * This position as a [FlatPoint] — drops the conformal component.
     *
     * Propagates at-infinity-ness (`az → pz`); component-identical to [carrier].
     */
    public val toFlat: FlatPoint by cached { FlatPoint(ax, ay, az) }

    /**
     * This direction as a [RoundDirection] — the refinement carrying the at-infinity
     * guarantee.
     *
     * Admits exactly the points at infinity: [ax], [ay], and [aw] carry over, and a round
     * weight that rounds to zero at the `SCALE` quantum is stored as exact `0.0`. An instance
     * that already is a [RoundDirection] returns itself. A finite point is a position, not a
     * direction, and is rejected.
     *
     * @throws FiniteRoundDirectionException when this point is finite — the round weight does
     *   not round to zero.
     */
    public val toRoundDirection: RoundDirection by cached {
        when {
            this is RoundDirection -> this
            isAtInfinity -> RoundDirection(ax, ay, aw)
            else -> throw FiniteRoundDirectionException(this)
        }
    }

    /**
     * Rounded identity: equal when every component matches at the `SCALE` quantum.
     *
     * Identity is exact-class with one carve-out: a [RoundDirection] and a plain [RoundPoint]
     * are the same kind of value — the refinement adds no data — and compare by components
     * alone, in both directions. Any other subclass instance and a plain [RoundPoint] never
     * compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's identity domain and its four
     *   components all match after rounding.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val compatible = other::class == this::class
                || (this::class == RoundPoint::class && other::class == RoundDirection::class)
                || (this::class == RoundDirection::class && other::class == RoundPoint::class)
        if (!compatible) return false
        other as RoundPoint
        return _ax == other._ax
                && _ay == other._ay
                && _az == other._az
                && _aw == other._aw
    }

    private val _hashCode by cached {
        var result = _ax.hashCode()
        result = 31 * result + _ay.hashCode()
        result = 31 * result + _az.hashCode()
        result = 31 * result + _aw.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "RoundPoint(ax=$ax, ay=$ay, az=$az, aw=$aw)"

    /** Returns the components rounded at the `SCALE` quantum — the debug-output convention of every primitive. */
    public fun toStringRounded(): String = "RoundPoint(ax=$_ax, ay=$_ay, az=$_az, aw=$_aw)"
}

/**
 * `RoundPoint - RoundPoint => Direction`<br>
 * Returns the free vector from [other]'s position to this point's position: the displacement that carries
 * [other] onto this point.
 *
 * Component-wise difference of the two positions, raw at full `Double` precision, as a flat [Direction] —
 * a displacement has no round content, so the conformal component [RoundPoint.aw] takes no part and a
 * sized or imaginary round point measures from its position all the same. Two [RoundDirection] operands
 * difference the same way, to a flat direction. Equal positions give the zero direction, which is legal.
 * A point at infinity — zero weight — carries a direction rather than a position, and the difference is
 * no displacement; gate with [RoundPoint.isAtInfinity].
 *
 * @receiver The point displaced to.
 * @param other The point displaced from.
 * @return The raw [Direction] from [other]'s position to this point's position.
 */
public operator fun RoundPoint.minus(other: RoundPoint): Direction = Direction(ax - other.ax, ay - other.ay)
