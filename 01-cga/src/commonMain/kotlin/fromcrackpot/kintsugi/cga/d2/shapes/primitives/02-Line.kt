package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * An oriented infinite line in the plane, or a weighted line at infinity — the 2D line.
 *
 * The grade-3 blade `gx·e423 + gy·e431 + gz·e412`, cutting the plane along
 * `gx·x + gy·y + gz = 0`. The constructor unitizes by the gradient magnitude `‖(gx, gy)‖`, so
 * a finite line stores its unit normal in ([gx], [gy]) and the origin's signed distance in
 * [gz]. Orientation is data: `−g` cuts the same point set with
 * the opposite orientation and [equals] distinguishes them ([canonical] for unoriented
 * equivalence). A weight that rounds to zero leaves the line at infinity with all three components
 * raw — at-infinity magnitudes are data, so `Line(0, 0, 5) != Line(0, 0, 1)`. Components are
 * stored raw at full `Double` precision; [equals]/[hashCode] compare rounded at the `SCALE`
 * quantum.
 */
public open class Line : Primitive {
    /** The `e423` component — the unit normal's x-component when finite. */
    public val gx: Double // e423

    /** The `e431` component — the unit normal's y-component when finite. */
    public val gy: Double // e431

    /**
     * The `e412` component — the origin's signed distance when finite;
     * the raw at-infinity magnitude otherwise.
     */
    public val gz: Double // e412

    private val _gx get() = round(gx)
    private val _gy get() = round(gy)
    private val _gz get() = round(gz)

    /*
    CTOR
     */

    /**
     * Builds the line `(gx, gy, gz)`, unitized and zero-sanitized.
     *
     * A g-part whose weight `‖(gx, gy)‖` doesn't round to zero is divided out to the unit
     * normal, leaving [gz] the origin's signed distance. A weight that rounds to zero at the
     * `SCALE` quantum leaves the line at infinity, all three components kept raw: the
     * magnitude [gz] is data. Signed zeroes sanitize to `+0.0`.
     *
     * @param gx The `e423` component: the normal's x-component.
     * @param gy The `e431` component: the normal's y-component.
     * @param gz The `e412` component: the offset term of `gx·x + gy·y + gz = 0`.
     */
    public constructor(gx: Double, gy: Double, gz: Double) {
        val flatWeightNorm = sqrt(gx * gx + gy * gy)

        if (round(flatWeightNorm) == 0.0) {
            this.gx = gx + 0.0
            this.gy = gy + 0.0
            this.gz = gz + 0.0
        } else { // Unitize: ĝ = g / flat_weight_norm(g)
            this.gx = gx / flatWeightNorm + 0.0
            this.gy = gy / flatWeightNorm + 0.0
            this.gz = gz / flatWeightNorm + 0.0
        }
    }

    /** Adopts components as-is, skipping unitization; [raw] disambiguates from the public ctor. */
    private constructor(gx: Double, gy: Double, gz: Double, raw: Unit) {
        this.gx = gx
        this.gy = gy
        this.gz = gz
    }

    /*
    PROPS
     */

    /**
     * Whether this line lies at infinity — a weighted horizon rather than a line in the plane.
     *
     * `true` when [gx] and [gy] both round to zero at the `SCALE` quantum; the remaining
     * magnitude [gz] is data. [normal] and [attitude] degenerate to the zero direction there —
     * gate on this predicate before reading them.
     */
    public val isAtInfinity: Boolean get() = _gx == 0.0 && _gy == 0.0

    /**
     * The distance from the origin `|gz|` — raw.
     *
     * The flat bulk norm: the unsigned origin distance for a unitized finite line, the
     * magnitude of a line at infinity.
     */
    public val norm: Double get() = abs(gz) // flat_bulk_norm

    /**
     * The unit normal as a free vector — [Direction] `(gx, gy)`.
     *
     * Perpendicular to the line, pointing toward the positive side (where
     * `gx·x + gy·y + gz > 0`). Degenerates to the zero direction at infinity.
     */
    public val normal: Direction by cached { Direction(gx, gy) }

    /**
     * The line's running direction as a free vector, 90° CCW from [normal].
     *
     * The attitude `att(g) = g ∨ e412` — the carrier tangent, one antigrade up: the free
     * vector [Direction] `(−gy, gx)`. Degenerates to the zero direction at infinity.
     */
    public val attitude: Direction by cached { Direction(-gy, gx) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [line] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a line computes its result as a plain [Line] and returns it
     * through this hook. This base implementation returns [line] itself; a subclass overrides
     * it to rebuild an instance of its own class around [line], and every such operation then
     * comes back as that class. The result's runtime class must equal the receiver's — the
     * operations check it and fail with a named error when a subclass misses the override.
     *
     * @param line The replacement geometry.
     * @return [line] itself; an override returns its own class rebuilt around [line].
     */
    public open fun copy(line: Line): Line = line

    /*
    IDENTITY
     */

    /**
     * The canonical representative of this line — useful for unoriented equivalence.
     *
     * `a.canonical == b.canonical` detects the same point set under opposed orientations. The
     * canonical orientation makes the first non-zero of `(gx, gy)` positive; the sign decision
     * reads rounded components, so `a == b` implies `a.canonical == b.canonical`. At infinity
     * the point set is unique, so canonicalization is scale-blind as well as sign-blind: an
     * at-infinity line whose [gz] doesn't round to zero at the `SCALE` quantum maps to
     * `Line(0, 0, 1)`; one that rounds all-zero stays itself. The representative is rebuilt
     * through [copy], so a subclass comes back as its own class; an already-canonical line
     * returns `this`.
     */
    public open val canonical: Line by cached {
        if (_gx == 0.0 && _gy == 0.0) {
            if (_gz == 0.0) this else copy(Line(0.0, 0.0, 1.0, Unit))
        } else {
            val s = if (_gx != 0.0) _gx.sign else _gy.sign
            if (s < 0) copy(Line(-gx + 0.0, -gy + 0.0, -gz + 0.0, Unit)) else this
        }
    }

    /**
     * Rounded, orientation-sensitive identity: equal when every component matches at the
     * `SCALE` quantum.
     *
     * `+e423 ≠ −e423` — the negated line cuts the same point set with the opposite orientation
     * and compares unequal; compare [canonical] on both sides for unoriented equivalence.
     * At-infinity magnitudes compare as data. Identity is exact-class: a subclass instance
     * and a plain [Line] never compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its three components
     *   all match after rounding.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Line
        return _gx == other._gx
                && _gy == other._gy
                && _gz == other._gz
    }

    private val _hashCode by cached {
        var result = _gx.hashCode()
        result = 31 * result + _gy.hashCode()
        result = 31 * result + _gz.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Line(gx=$gx, gy=$gy, gz=$gz)"

    /** Returns the components rounded at the `SCALE` quantum — the debug-output convention of every primitive. */
    public fun toStringRounded(): String = "Line(gx=$_gx, gy=$_gy, gz=$_gz)"
}

/**
 * `-Line => Line`<br>
 * Returns this line with the opposite orientation — the same cut through the plane, positive
 * side swapped.
 *
 * Negates all three components, raw at full `Double` precision: for a finite line the unit
 * [Line.normal] and the running [Line.attitude] both flip and the origin's signed distance
 * changes sign; a line at infinity negates its raw magnitude, which is data. Identity is
 * orientation-sensitive, so a line and its negation compare unequal (the all-zero line at
 * infinity being its own negation); compare [Line.canonical] for unoriented equivalence —
 * `(-line).canonical == line.canonical`. The result is the receiver's own class: the negated
 * geometry returns through [Line.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Line] to negate.
 * @param T The receiver's type, preserved in the result.
 * @return The negated line, as the receiver's class.
 */
public operator fun <T : Line> T.unaryMinus(): T =
    narrowed(
        copy(
            Line(
                -gx, // e423
                -gy, // e431
                -gz, // e412
            )
        )
    )
