package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.narrowed
import kotlin.math.sqrt

/**
 * Bounded line segment between its endpoints.
 *
 * - Answers against a `Segment` respect its bounds. [toDipole] switches to the unbounded blade.
 * - [start] and [end] return the very instances the constructor was given.
 * - Identity is the wrapped dipole's. It rounds at the `SCALE` quantum and counts orientation.
 */
public open class Segment internal constructor(
    private val d: Dipole,
    private val cachedStart: RoundPoint? = null,
    private val cachedEnd: RoundPoint? = null,
) : Composite {
    /** The `e23` component: the x-component of the carrier line's unit normal, when finite. See [Dipole.gx]. */
    public val gx: Double get() = d.gx // e23

    /** The `e31` component: the y-component of the carrier line's unit normal, when finite. See [Dipole.gy]. */
    public val gy: Double get() = d.gy // e31

    /** The `e12` component: the carrier line's signed distance from the origin, when finite. See [Dipole.gz]. */
    public val gz: Double get() = d.gz // e12

    /** The `e41` component: the x-component of the flat bulk. See [Dipole.px]. */
    public val px: Double get() = d.px // e41

    /** The `e42` component: the y-component of the flat bulk. See [Dipole.py]. */
    public val py: Double get() = d.py // e42

    /** The `e43` component: minus the center's signed offset along [attitude], when finite. See [Dipole.pz]. */
    public val pz: Double get() = d.pz // e43

    /*
    CTOR
    */

    /**
     * Builds the segment between two flat points.
     *
     * @param start The first endpoint.
     * @param end The second endpoint.
     * @throws DegenerateDipoleException when the endpoints coincide at the `SCALE` quantum.
     */
    public constructor(start: FlatPoint, end: FlatPoint) : this(start.toRound, end.toRound)

    /**
     * Builds the segment between two round points, keeping the instances as [start] and [end].
     *
     * @param start The first endpoint.
     * @param end The second endpoint.
     * @throws DegenerateDipoleException when the endpoints coincide at the `SCALE` quantum.
     */
    public constructor(start: RoundPoint, end: RoundPoint) : this(start join end, start, end)

    /**
     * Builds a copy of [segment] that shares its stored geometry, the endpoints' instances included.
     *
     * - Never throws. Any existing segment copies as is, even one at infinity or an imaginary one.
     *
     * @param segment The segment to copy.
     */
    protected constructor(segment: Segment) : this(segment.d, segment.cachedStart, segment.cachedEnd)

    /*
    MATH
     */

    // radius² two ways:
    //   Pythagoras on the endpoints: precise
    //   d.radius2, the blade formula: subtracts ~coord³ terms; noise beats SCALE past coords ~10⁴
    // This gate asks "may I use Pythagoras?". null = "no, fall back to d.radius2":
    //   no cached endpoints → d.start/d.end derive from radius2 itself (circular)
    //   az != 1             → at infinity, chord distance is meaningless
    //   aw != lift          → semantic-aw point, true radius² isn't the chord
    // Checks are bit-exact on purpose. The lift check reuses the RoundPoint ctor's exact
    // expression, so plain points always pass. Don't round these.
    private val liftedRadius2: Double? get() {
        val s = cachedStart ?: return null
        val e = cachedEnd ?: return null
        if (s.az != 1.0 || e.az != 1.0) return null
        if (s.aw != (s.ax * s.ax + s.ay * s.ay) / 2) return null
        if (e.aw != (e.ax * e.ax + e.ay * e.ay) / 2) return null
        val dx = e.ax - s.ax
        val dy = e.ay - s.ay
        return (dx * dx + dy * dy) / 4
    }

    /**
     * Whether this segment is imaginary, with a negative squared radius and no real endpoints.
     *
     * - Always `false` for a segment built from two finite plain points.
     */
    public val isImaginary: Boolean get() = if (liftedRadius2 != null) false else d.isImaginary

    /** The midpoint of the endpoints. It degenerates at infinity. See [Dipole.center]. */
    public val center: RoundPoint get() = d.center

    /**
     * Half the raw distance between the endpoints, never negative. For an imaginary segment, it is
     * the magnitude of the imaginary radius.
     */
    public val radius: Double get() = liftedRadius2?.let(::sqrt) ?: d.radius

    /**
     * The raw, signed square of half the endpoint distance. It is negative for an imaginary
     * segment.
     */
    public val radius2: Double get() = liftedRadius2 ?: d.radius2

    /** The raw distance between the endpoints, twice [radius]. */
    public val length: Double get() = radius * 2

    /**
     * The first endpoint. It is the very instance the constructor was given.
     *
     * - A segment built through [Dipole.toSegment] derives it from the blade. It degenerates at infinity.
     */
    public val start: RoundPoint get() = cachedStart ?: d.start

    /**
     * The second endpoint. It is the very instance the constructor was given.
     *
     * - A segment built through [Dipole.toSegment] derives it from the blade. It degenerates at infinity.
     */
    public val end: RoundPoint get() = cachedEnd ?: d.end

    /** The line through both endpoints. See [Dipole.carrier]. */
    public val carrier: Line get() = d.carrier

    /** The perpendicular bisector of the endpoints. See [Dipole.cocarrier]. */
    public val cocarrier: Line get() = d.cocarrier

    /**
     * The smallest circle containing this segment, with the endpoints as a diameter. See
     * [Dipole.container].
     *
     * @throws DegenerateCircleException when the segment lies at infinity.
     */
    public val container: Circle get() = d.container

    /** The direction from [start] toward [end], as a round point at infinity. See [Dipole.attitude]. */
    public val attitude: RoundDirection get() = d.attitude

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [segment]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param segment The replacement geometry.
     * @return [segment] itself. An override returns its own class rebuilt around [segment].
     */
    public open fun copy(segment: Segment): Segment = segment

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this segment, the same for both orientations.
     *
     * - Returns `this` when already canonical. Otherwise the endpoints swap.
     */
    public open val canonical: Segment by cached {
        val dc = d.canonical
        if (dc === d) this else copy(Segment(dc, cachedEnd, cachedStart))
    }

    /** The wrapped [Dipole], the bridge back to pure CGA. Answers against it ignore the segment's bounds. */
    public val toDipole: Dipole get() = d

    /**
     * Compares the wrapped dipoles at the `SCALE` quantum.
     *
     * - Swapped endpoints compare unequal. Compare [canonical] on both sides for unoriented equivalence.
     * - Classes must match exactly. A subclass instance never equals a plain [Segment].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and an equal wrapped dipole.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Segment
        return d == other.d
    }

    /** Returns the wrapped dipole's rounded hash, consistent with [equals]. */
    override fun hashCode(): Int = d.hashCode()

    /** Returns the endpoints at full stored precision. */
    override fun toString(): String = "Segment(start=$start, end=$end)"

    /** Returns the endpoints rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Segment(start=${start.toStringRounded()}, end=${end.toStringRounded()})"
}

/**
 * `-Segment => Segment`<br>
 * Returns this segment with its endpoints swapped. The chord stays, the orientation flips.
 *
 * - Compares unequal to the original. Compare [Segment.canonical] for unoriented equivalence.
 *
 * @receiver The segment to reverse.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed segment.
 */
public operator fun <T : Segment> T.unaryMinus(): T =
    // At infinity the derived endpoints collapse to the zero point, leaving no wedge to rebuild.
    narrowed(copy(if (toDipole.isAtInfinity) (-toDipole).toSegment else Segment(end, start)))
