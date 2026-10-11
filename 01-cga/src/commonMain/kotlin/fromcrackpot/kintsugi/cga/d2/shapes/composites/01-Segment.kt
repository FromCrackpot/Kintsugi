package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.narrowed
import kotlin.math.sqrt

/**
 * Bounded line segment between its endpoints.
 *
 * A [Composite] wrapping the [Dipole] of its endpoints and forwarding its algebra
 * accessors; [toDipole] is the bridge back to pure CGA, symmetric with [Dipole.toSegment].
 * Geometric answers against a `Segment` respect its bounds — the chord itself, not the
 * infinite carrier — while answers against the wrapped blade stay total.
 *
 * The endpoint constructors keep the [RoundPoint] instances they are given: [start]/[end]
 * return exactly those objects, so composites built on shared vertices ([Polygon]'s edges)
 * share them by reference. Identity is blade-based: [equals]/[hashCode] compare the
 * wrapped dipoles, rounded at the `SCALE` quantum and orientation-sensitive ([canonical] for
 * unoriented equivalence).
 */
public open class Segment internal constructor(
    private val d: Dipole,
    private val cachedStart: RoundPoint? = null,
    private val cachedEnd: RoundPoint? = null,
) : Composite {
    /** The `e23` component — the carrier line's unit-normal x-component when finite; see [Dipole.gx]. */
    public val gx: Double get() = d.gx // e23

    /** The `e31` component — the carrier line's unit-normal y-component when finite; see [Dipole.gy]. */
    public val gy: Double get() = d.gy // e31

    /** The `e12` component — the carrier line's signed origin distance when finite; see [Dipole.gz]. */
    public val gz: Double get() = d.gz // e12

    /** The `e41` component of the flat bulk; see [Dipole.px]. */
    public val px: Double get() = d.px // e41

    /** The `e42` component of the flat bulk; see [Dipole.py]. */
    public val py: Double get() = d.py // e42

    /** The `e43` component — minus the center's signed offset along [attitude] when finite; see [Dipole.pz]. */
    public val pz: Double get() = d.pz // e43

    /*
    CTOR
    */

    /**
     * Builds the segment between two flat points.
     *
     * Lifts each endpoint to a [RoundPoint] and delegates to the round-point constructor, which
     * caches the lifts as [Segment.start]/[Segment.end]. Cannot produce an imaginary segment:
     * two distinct flat points always wedge into a real blade.
     *
     * @param start The first endpoint.
     * @param end The second endpoint.
     * @throws DegenerateDipoleException When the endpoints coincide at the `SCALE` quantum —
     *   coincident points span no segment.
     */
    public constructor(start: FlatPoint, end: FlatPoint) : this(start.toRound, end.toRound)

    /**
     * Builds the segment between two round points.
     *
     * Wedges the endpoints into the wrapped [Dipole] and keeps them, so
     * [Segment.start]/[Segment.end] return these very instances. Imaginary or at-infinity
     * endpoints flow through to the blade — first-class data, never an error.
     *
     * @param start The first endpoint.
     * @param end The second endpoint.
     * @throws DegenerateDipoleException When the endpoints coincide at the `SCALE` quantum —
     *   coincident points span no segment.
     */
    public constructor(start: RoundPoint, end: RoundPoint) : this(start join end, start, end)

    /*
    MATH
     */

    // radius² two ways: Pythagoras on the endpoints (precise), or d.radius2 (blade formula —
    // subtracts huge ~coord³ terms, noise beats the SCALE quantum past coords ~10⁴).
    // This gate asks "may I use Pythagoras?"; null = "no, fall back to d.radius2":
    //   no cached endpoints → only derived d.start/d.end, computed via radius2 itself — circular
    //   az != 1             → at infinity, chord distance is meaningless
    //   aw != lift          → semantic-aw point, true radius² isn't the chord
    // Checks are bit-exact on purpose: the lift check reuses the RoundPoint ctor's exact
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
     * Whether this segment is imaginary — a negative squared radius, no real endpoints.
     *
     * Always `false` for a segment built from finite endpoints on the null lift — two distinct
     * plain points always span a real segment. Endpoints at infinity or carrying a semantic
     * `aw` forward to [Dipole.isImaginary], which rounds.
     */
    public val isImaginary: Boolean get() = if (liftedRadius2 != null) false else d.isImaginary

    /** The midpoint of the endpoints — degenerates at infinity; see [Dipole.center]. */
    public val center: RoundPoint get() = d.center

    /**
     * Half the distance between the endpoints for a real segment — raw, non-negative.
     *
     * The square root of [radius2]'s magnitude; for an imaginary segment the magnitude of the
     * imaginary radius. See [Dipole.radius].
     */
    public val radius: Double get() = liftedRadius2?.let(::sqrt) ?: d.radius

    /**
     * The squared half-distance between the endpoints — raw and signed; negative for an
     * imaginary segment.
     *
     * For a segment built from finite endpoints this is computed directly from their
     * coordinates, which stays precise at large coordinates where the algebraic
     * [Dipole.radius2] drifts. Endpoints at infinity or carrying a semantic `aw` fall back to
     * [Dipole.radius2].
     */
    public val radius2: Double get() = liftedRadius2 ?: d.radius2

    /**
     * The distance between the segment's endpoints for a real segment — twice [radius], raw
     * and non-negative.
     *
     * For an imaginary segment, twice the magnitude of the imaginary radius; see [radius].
     */
    public val length: Double get() = radius * 2

    /**
     * The first endpoint.
     *
     * The exact [RoundPoint] the constructor was given, shared by reference with composites
     * built on the same vertex. A segment built via [Dipole.toSegment] has no original
     * and derives it from the blade ([Dipole.start]); degenerates at infinity.
     */
    public val start: RoundPoint get() = cachedStart ?: d.start

    /**
     * The second endpoint.
     *
     * The exact [RoundPoint] the constructor was given, shared by reference with composites
     * built on the same vertex. A segment built via [Dipole.toSegment] has no original
     * and derives it from the blade ([Dipole.end]); degenerates at infinity.
     */
    public val end: RoundPoint get() = cachedEnd ?: d.end

    /** The line through both endpoints; see [Dipole.carrier]. */
    public val carrier: Line get() = d.carrier

    /** The perpendicular bisector of the endpoints; see [Dipole.cocarrier]. */
    public val cocarrier: Line get() = d.cocarrier

    /**
     * The smallest circle containing this segment — the endpoints as a diameter; see
     * [Dipole.container].
     *
     * @throws DegenerateCircleException At infinity — the collapsed round weight leaves the
     *   [Circle] constructor nothing to unitize by.
     */
    public val container: Circle get() = d.container

    /** The direction from [start] toward [end] — a round point at infinity; see [Dipole.attitude]. */
    public val attitude: RoundDirection get() = d.attitude

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [segment] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a segment computes its result as a plain [Segment] and
     * returns it through this hook. This base implementation returns [segment] itself; a
     * subclass overrides it to rebuild an instance of its own class around [segment], and
     * every such operation then comes back as that class. The result's runtime class must
     * equal the receiver's — the operations check it and fail with a named error when a
     * subclass misses the override.
     *
     * @param segment The replacement geometry.
     * @return [segment] itself; an override returns its own class rebuilt around [segment].
     */
    public open fun copy(segment: Segment): Segment = segment

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this segment — useful for unoriented equivalence.
     *
     * Follows [Dipole.canonical]: an already-canonical blade returns `this`; otherwise the
     * blade is negated, which exchanges the endpoints exactly, so [start] and [end] swap. The
     * representative is rebuilt through [copy], so a subclass comes back as its own class.
     */
    public open val canonical: Segment by cached {
        val dc = d.canonical
        if (dc === d) this else copy(Segment(dc, cachedEnd, cachedStart))
    }

    /**
     * The wrapped [Dipole] — the bridge back to pure CGA.
     *
     * Returns the held instance (`segment.toDipole === segment.toDipole`). Selecting `Dipole`
     * makes geometric answers run against the whole carrier instead of the chord's bounds;
     * symmetric with [Dipole.toSegment].
     */
    public val toDipole: Dipole get() = d

    /**
     * Rounded, orientation-sensitive identity: equal when the wrapped dipoles are equal.
     *
     * Compares the six blade components at the `SCALE` quantum — nothing else. Swapping the
     * endpoints compares unequal; compare [canonical] on both sides for unoriented equivalence.
     * Identity is exact-class: a subclass instance and a plain [Segment] never compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its wrapped dipole
     *   equals this one's.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Segment
        return d == other.d
    }

    /** Returns the wrapped dipole's hash — rounded, consistent with [equals]. */
    override fun hashCode(): Int = d.hashCode()

    /** Returns the endpoints at raw precision — full stored values, no rounding. */
    override fun toString(): String = "Segment(start=$start, end=$end)"

    /** Returns the endpoints rounded at the `SCALE` quantum — the debug-output convention of every shape. */
    public fun toStringRounded(): String = "Segment(start=${start.toStringRounded()}, end=${end.toStringRounded()})"
}

/**
 * `-Segment => Segment`<br>
 * Returns this segment with its endpoints exchanged — the same chord, opposite orientation.
 *
 * Re-wedges [Segment.end] with [Segment.start], negating the wrapped blade — the wedge of two
 * points flips sign with their order — so the negation agrees with the dipole's:
 * `(-segment).toDipole == -(segment.toDipole)`. The endpoint instances are kept, exchanged; a
 * segment at infinity has no endpoints to exchange and negates the wrapped blade directly,
 * raw components as data. Identity is orientation-sensitive, so a segment and its negation
 * compare unequal; compare [Segment.canonical] for unoriented equivalence. The result is the
 * receiver's own class: the reversed geometry returns through [Segment.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Segment] to reverse.
 * @param T The receiver's type, preserved in the result.
 * @return The reversed segment, as the receiver's class.
 */
public operator fun <T : Segment> T.unaryMinus(): T =
    // At infinity the derived endpoints collapse to the zero point — no wedge to rebuild.
    narrowed(copy(if (toDipole.isAtInfinity) (-toDipole).toSegment else Segment(end, start)))
