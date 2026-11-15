package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteRoundDirectionException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.round
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * A point in the plane, optionally carrying a size.
 *
 * - Finite points are unitized, so [ax] and [ay] are the Euclidean coordinates.
 * - [aw] sets the size: [radius2] is zero for a plain point, negative for an imaginary one.
 * - Stored raw. [equals] and [hashCode] round at the `SCALE` quantum, [aw] included.
 */
public open class RoundPoint : Primitive {
    /** The `e1` component: the Euclidean x-coordinate, when finite. */
    public val ax: Double // e1

    /** The `e2` component: the Euclidean y-coordinate, when finite. */
    public val ay: Double // e2

    /** The `e3` round weight: `1` when finite; raw and rounding to `0` at infinity. */
    public val az: Double // e3

    /** The `e4` conformal component: `(ax² + ay²)/2` for a plain Euclidean point. */
    public val aw: Double // e4

    private val _ax get() = round(ax)
    private val _ay get() = round(ay)
    private val _az get() = round(az)
    private val _aw get() = round(aw)

    /*
    CTOR
     */

    /**
     * Builds the round point `(ax, ay, az, aw)`, unitized, with `-0.0` cleared to `0.0`.
     *
     * - A round weight that rounds to zero at the `SCALE` quantum stays raw. The direction's magnitude is data.
     *
     * @param ax The `e1` component.
     * @param ay The `e2` component.
     * @param az The `e3` round weight. `0` places the point at infinity.
     * @param aw The `e4` conformal component. `(ax² + ay²)/2` makes a plain Euclidean point.
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
     * Builds the plain Euclidean point `(ax, ay)`, with no size and [radius2] exactly zero.
     *
     * @param ax The Euclidean x-coordinate.
     * @param ay The Euclidean y-coordinate.
     */
    public constructor(ax: Double, ay: Double) : this(ax, ay, az = 1.0, aw = (ax * ax + ay * ay) / 2.0)

    /*
    PROPS
     */

    /**
     * Whether this point lies at infinity, as a direction with magnitude rather than a position.
     * True when [az] rounds to zero at the `SCALE` quantum.
     */
    public val isAtInfinity: Boolean get() = _az == 0.0

    /**
     * Whether this point is imaginary. True when [radius2] rounds below zero at the `SCALE` quantum.
     *
     * - An imaginary point is valid data, not an error.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * The raw Euclidean magnitude `√(ax² + ay²)`: the origin distance when finite; the free
     * magnitude at infinity.
     */
    public val norm: Double get() = sqrt(ax * ax + ay * ay) // round_bulk_norm / center_norm

    /**
     * The raw, signed squared radius: zero for a plain point, negative for an imaginary one.
     *
     * - Test imaginary-ness with [isImaginary], which rounds. This raw value is unstable near zero.
     */
    public val radius2: Double get() = 2 * az * aw - ax * ax - ay * ay + 0.0

    /**
     * The raw radius magnitude `√(|radius2|)`, never negative. Check [isImaginary] where the sign
     * matters.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /** The attitude, degenerate for a point. It is just the weight [az]. */
    public val attitude: Double get() = az // Scalar

    /** The carrier: the flat point at this position, component-identical to [toFlat]. */
    public val carrier: FlatPoint by cached { FlatPoint(ax, ay, az) }

    /** The co-carrier, degenerate for a point. It is just the weight [az]. */
    public val cocarrier: Double get() = az // AntiScalar

    /**
     * The smallest circle containing this point. A plain point gets zero radius.
     *
     * - Always inside-out. An imaginary point gets an imaginary container.
     *
     * @throws DegenerateCircleException when the point lies at infinity.
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
     * Returns this object rebuilt around the replacement geometry [roundPoint]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param roundPoint The replacement geometry.
     * @return [roundPoint] itself. An override returns its own class rebuilt around [roundPoint].
     */
    public open fun copy(roundPoint: RoundPoint): RoundPoint = roundPoint

    /*
    IDENTITY
    */

    /** This position as a [FlatPoint], without the conformal component. A point at infinity stays there. */
    public val toFlat: FlatPoint by cached { FlatPoint(ax, ay, az) }

    /**
     * This direction as a [RoundDirection], with its weight stored as exact `0.0`. A
     * [RoundDirection] returns itself.
     *
     * @throws FiniteRoundDirectionException when this point is finite.
     */
    public val toRoundDirection: RoundDirection by cached {
        when {
            this is RoundDirection -> this
            isAtInfinity -> RoundDirection(ax, ay, aw)
            else -> throw FiniteRoundDirectionException(this)
        }
    }

    /**
     * Compares the four components rounded at the `SCALE` quantum, [aw] included.
     *
     * - A [RoundDirection] and a plain [RoundPoint] compare by components. Other subclasses never equal a plain one.
     *
     * @param other The object to compare against.
     * @return `true` when [other] shares this identity domain and every rounded component matches.
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

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "RoundPoint(ax=$ax, ay=$ay, az=$az, aw=$aw)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "RoundPoint(ax=$_ax, ay=$_ay, az=$_az, aw=$_aw)"
}

/**
 * `RoundPoint - RoundPoint => Direction`<br>
 * Returns the free vector from [other]'s position to this point's. It is the displacement that
 * carries [other] onto this point.
 *
 * - Reads positions only, so a sized or imaginary point measures from its position.
 * - Two points at infinity subtract as free vectors.
 *
 * @receiver The point displaced to.
 * @param other The point displaced from.
 * @return The raw flat direction between the positions.
 * @throws AtInfinityException when exactly one of the points lies at infinity.
 */
public operator fun RoundPoint.minus(other: RoundPoint): Direction {
    if (isAtInfinity != other.isAtInfinity) {
        throw AtInfinityException("no displacement between a position and a point at infinity: $this, $other")
    }
    return Direction(ax - other.ax, ay - other.ay)
}
