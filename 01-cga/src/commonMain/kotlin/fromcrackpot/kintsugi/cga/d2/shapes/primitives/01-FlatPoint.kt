package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteDirectionException
import fromcrackpot.kintsugi.cga.round
import kotlin.math.sqrt

/**
 * A weighted position in the plane, or a free vector when at infinity — the 2D flat point.
 *
 * The grade-2 blade `px·e41 + py·e42 + pz·e43`. The constructor unitizes finite points to
 * `pz = 1`, so the stored [px], [py] are the Euclidean coordinates. A weight that rounds to
 * zero places the point at infinity: pure magnitude and orientation with no position (see
 * [Direction]). Components are stored raw at full `Double` precision; [equals]/[hashCode]
 * compare rounded at the `SCALE` quantum.
 */
public open class FlatPoint : Primitive {
    /** The `e41` component — the Euclidean x-coordinate when finite. */
    public val px: Double // e41

    /** The `e42` component — the Euclidean y-coordinate when finite. */
    public val py: Double // e42

    /** The `e43` weight — `1` when finite; rounds to `0` at infinity (kept raw). */
    public val pz: Double // e43

    private val _px get() = round(px)
    private val _py get() = round(py)
    private val _pz get() = round(pz)

    /*
    CTOR
     */

    /**
     * Builds the flat point `(px, py, pz)`, unitized and zero-sanitized.
     *
     * With the default weight this is the Euclidean point ([px], [py]). A finite non-unit
     * weight is divided out to the canonical `pz = 1`. A weight that rounds to zero at the
     * `SCALE` quantum is kept raw, all three components untouched: the point is at infinity
     * in the direction `(px, py)` and its magnitude is data. Signed zeroes sanitize to `+0.0`.
     *
     * @param px The `e41` component.
     * @param py The `e42` component.
     * @param pz The `e43` weight; defaults to `1.0`, the already-unitized finite point.
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
     * Whether this point lies at infinity — a free vector rather than a position.
     *
     * `true` when [pz] rounds to zero at the `SCALE` quantum.
     */
    public val isAtInfinity: Boolean get() = _pz == 0.0

    /**
     * The Euclidean magnitude `√(px² + py²)` — raw.
     *
     * The flat bulk norm: the distance from the origin for a unitized finite point, the
     * free vector's length at infinity.
     */
    public val norm: Double get() = sqrt(px * px + py * py) // flat_bulk_norm

    /**
     * The attitude, degenerate for a point — pure weight, no direction.
     *
     * `att(p) = p ∨ e412`, one antigrade up: a [RoundDirection] carrying only the flat bulk
     * `−pz·e4`.
     */
    public val attitude: RoundDirection by cached { RoundDirection(0.0, 0.0, aw = -pz) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [flatPoint] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a flat point computes its result as a plain [FlatPoint] and
     * returns it through this hook. This base implementation returns [flatPoint] itself; a
     * subclass overrides it to rebuild an instance of its own class around [flatPoint], and
     * every such operation then comes back as that class. The result's runtime class must
     * equal the receiver's — the operations check it and fail with a named error when a
     * subclass misses the override.
     *
     * @param flatPoint The replacement geometry.
     * @return [flatPoint] itself; an override returns its own class rebuilt around [flatPoint].
     */
    public open fun copy(flatPoint: FlatPoint): FlatPoint = flatPoint

    /*
    IDENTITY
    */

    /**
     * This position as a [RoundPoint] — the conformal lift.
     *
     * A finite point lifts with round bulk `aw = (px² + py²)/2`, computed on the unitized
     * components; at infinity the lift is invalid, so the round weight and bulk are zero
     * (`az = aw = 0`) and the at-infinity magnitude carries over into `(ax, ay)`.
     */
    public val toRound: RoundPoint by cached {
        if (isAtInfinity) RoundPoint(px, py, 0.0, 0.0)
        else RoundPoint(px, py, pz, (px * px + py * py) / 2.0)
    }

    /**
     * This free vector as a [Direction] — the refinement carrying the at-infinity guarantee.
     *
     * Admits exactly the points at infinity: [px] and [py] carry over, and a weight that
     * rounds to zero at the `SCALE` quantum is stored as exact `0.0`. An instance that
     * already is a [Direction] returns itself. A finite point is a position, not a free
     * vector, and is rejected.
     *
     * @throws FiniteDirectionException when this point is finite — the weight does not round
     *   to zero.
     */
    public val toDirection: Direction by cached {
        when {
            this is Direction -> this
            isAtInfinity -> Direction(px, py)
            else -> throw FiniteDirectionException(this)
        }
    }

    /**
     * Rounded identity: equal when every component matches at the `SCALE` quantum.
     *
     * Raw components may drift within the quantum and still compare equal. The weight
     * participates: a point at infinity never equals a finite point, and at-infinity
     * magnitudes compare as data. Identity is exact-class with one carve-out: a [Direction]
     * and a plain [FlatPoint] are the same kind of value — the refinement adds no data — and
     * compare by components alone, in both directions. Any other subclass instance and a
     * plain [FlatPoint] never compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's identity domain and its three
     *   components all match after rounding.
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

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "FlatPoint(px=$px, py=$py, pz=$pz)"

    /** Returns the components rounded at the `SCALE` quantum — the debug-output convention of every primitive. */
    public fun toStringRounded(): String = "FlatPoint(px=$_px, py=$_py, pz=$_pz)"
}

/**
 * `FlatPoint - FlatPoint => Direction`<br>
 * Returns the free vector from [other] to this point: the displacement that carries [other] onto this point.
 *
 * Component-wise difference of the two positions, raw at full `Double` precision, as a [Direction] — the
 * weight takes no part, so the result lies at infinity by construction. Equal points give the zero
 * direction, which is legal. A point at infinity — zero weight — carries a free vector rather than a
 * position, and the difference is no displacement; gate with [FlatPoint.isAtInfinity].
 *
 * @receiver The point displaced to.
 * @param other The point displaced from.
 * @return The raw [Direction] from [other] to this point.
 */
public operator fun FlatPoint.minus(other: FlatPoint): Direction = Direction(px - other.px, py - other.py)
