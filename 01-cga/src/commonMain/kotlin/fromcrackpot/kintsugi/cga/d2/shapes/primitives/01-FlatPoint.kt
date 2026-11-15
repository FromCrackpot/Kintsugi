package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteDirectionException
import fromcrackpot.kintsugi.cga.round
import kotlin.math.sqrt

/**
 * A weighted position in the plane, or a free vector when at infinity.
 *
 * - Finite points are unitized, so [px] and [py] are the Euclidean coordinates.
 * - A weight that rounds to zero puts the point at infinity, as a free vector (see [Direction]).
 * - Stored raw. [equals] and [hashCode] round at the `SCALE` quantum.
 */
public open class FlatPoint : Primitive {
    /** The `e41` component: the Euclidean x-coordinate, when finite. */
    public val px: Double // e41

    /** The `e42` component: the Euclidean y-coordinate, when finite. */
    public val py: Double // e42

    /** The `e43` weight: `1` when finite; raw and rounding to `0` at infinity. */
    public val pz: Double // e43

    private val _px get() = round(px)
    private val _py get() = round(py)
    private val _pz get() = round(pz)

    /*
    CTOR
     */

    /**
     * Builds the flat point `(px, py, pz)`, unitized, with `-0.0` cleared to `0.0`.
     *
     * - A weight that rounds to zero at the `SCALE` quantum stays raw. The direction's magnitude is data.
     *
     * @param px The `e41` component.
     * @param py The `e42` component.
     * @param pz The `e43` weight. The default `1.0` gives an already-unitized finite point.
     */
    public constructor(px: Double, py: Double, pz: Double = 1.0) {
        if (round(pz) == 0.0 || pz == 1.0) {
            this.px = px + 0.0
            this.py = py + 0.0
            this.pz = pz + 0.0
        } else { // Unitize: p̂ = p / flat_weight_norm(p)
            this.px = px / pz + 0.0
            this.py = py / pz + 0.0
            this.pz = 1.0
        }
    }

    /*
    PROPS
     */

    /**
     * Whether this point lies at infinity, as a free vector rather than a position. True when [pz]
     * rounds to zero at the `SCALE` quantum.
     */
    public val isAtInfinity: Boolean get() = _pz == 0.0

    /**
     * The raw Euclidean magnitude `√(px² + py²)`: the origin distance when finite; the free
     * vector's length at infinity.
     */
    public val norm: Double get() = sqrt(px * px + py * py) // flat_bulk_norm

    /** The attitude, degenerate for a point. It holds pure weight and no direction. */
    public val attitude: RoundDirection by cached { RoundDirection(0.0, 0.0, aw = -pz) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [flatPoint]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param flatPoint The replacement geometry.
     * @return [flatPoint] itself. An override returns its own class rebuilt around [flatPoint].
     */
    public open fun copy(flatPoint: FlatPoint): FlatPoint = flatPoint

    /*
    IDENTITY
    */

    /**
     * This position lifted to a [RoundPoint]. At infinity, the round weight and bulk are zero.
     */
    public val toRound: RoundPoint by cached {
        if (isAtInfinity) RoundPoint(px, py, 0.0, 0.0)
        else RoundPoint(px, py, pz, (px * px + py * py) / 2.0)
    }

    /**
     * This free vector as a [Direction], with its weight stored as exact `0.0`. A [Direction]
     * returns itself.
     *
     * @throws FiniteDirectionException when this point is finite.
     */
    public val toDirection: Direction by cached {
        when {
            this is Direction -> this
            isAtInfinity -> Direction(px, py)
            else -> throw FiniteDirectionException(this)
        }
    }

    /**
     * Compares the three components rounded at the `SCALE` quantum.
     *
     * - A point at infinity never equals a finite one. At-infinity magnitudes compare as data.
     * - A [Direction] and a plain [FlatPoint] compare by components. Other subclasses never equal a plain one.
     *
     * @param other The object to compare against.
     * @return `true` when [other] shares this identity domain and every rounded component matches.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val compatible = other::class == this::class
                || (this::class == FlatPoint::class && other::class == Direction::class)
                || (this::class == Direction::class && other::class == FlatPoint::class)
        if (!compatible) return false
        other as FlatPoint
        return _px == other._px
                && _py == other._py
                && _pz == other._pz
    }

    private val _hashCode by cached {
        var result = _px.hashCode()
        result = 31 * result + _py.hashCode()
        result = 31 * result + _pz.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "FlatPoint(px=$px, py=$py, pz=$pz)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "FlatPoint(px=$_px, py=$_py, pz=$_pz)"
}

/**
 * `FlatPoint - FlatPoint => Direction`<br>
 * Returns the free vector from [other] to this point. It is the displacement that carries [other]
 * onto this point.
 *
 * - Two points at infinity subtract as free vectors.
 *
 * @receiver The point displaced to.
 * @param other The point displaced from.
 * @return The raw direction from [other] to this point.
 * @throws AtInfinityException when exactly one of the points lies at infinity.
 */
public operator fun FlatPoint.minus(other: FlatPoint): Direction {
    if (isAtInfinity != other.isAtInfinity) {
        throw AtInfinityException("no displacement between a position and a point at infinity: $this, $other")
    }
    return Direction(px - other.px, py - other.py)
}
