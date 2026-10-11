package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * A circle in the plane, oriented by which side counts as its interior — the 2D circle.
 *
 * The grade-3 blade `cx·e423 + cy·e431 + cz·e412 + cw·e321`, the wedge of three round points.
 * The constructor unitizes by the round weight `|cw|`, preserving its sign — the orientation:
 * `cw = −1` is the classic circle whose interior is the bounded disk, `cw = +1` the inside-out
 * circle whose interior is everything outside. Center and radius are first-class views
 * ([center], [radius]); a negative [radius2] makes the circle imaginary — no real points,
 * first-class data, never an error (see [isImaginary]). Orientation is data: negating all four
 * components keeps the point set and swaps the interior, and [equals] distinguishes the two
 * ([canonical] for unoriented equivalence). A round weight that rounds to zero is not a circle
 * but the line `(cx, cy, cz)` — the constructor throws rather than build one. Components are
 * stored raw at full `Double` precision; [equals]/[hashCode] compare rounded at the `SCALE`
 * quantum.
 */
public open class Circle : Primitive {
    /** The `e423` component — the center's x-coordinate for a classic circle; negated when inside-out. */
    public val cx: Double // e423

    /** The `e431` component — the center's y-coordinate for a classic circle; negated when inside-out. */
    public val cy: Double // e431

    /**
     * The `e412` component — minus half the origin's power with respect to the circle,
     * `(radius² − ‖center‖²)/2`, for a classic circle; negated when inside-out.
     */
    public val cz: Double // e412

    /**
     * The `e321` round weight — `−1` for a classic circle whose interior is the bounded disk,
     * `+1` for an inside-out circle whose interior is everything outside it.
     */
    public val cw: Double // e321

    private val _cx get() = round(cx)
    private val _cy get() = round(cy)
    private val _cz get() = round(cz)
    private val _cw get() = round(cw)

    /*
    CTOR
    */

    /**
     * Builds the circle `(cx, cy, cz, cw)`, unitized and zero-sanitized.
     *
     * A round weight [cw] that doesn't round to zero is divided out of every component by its
     * magnitude, preserving its sign — the orientation survives unitization as `cw = ±1`. A
     * weight that rounds to zero at the `SCALE` quantum is not a circle — the blade is the line
     * `(cx, cy, cz)` — and the constructor throws. Signed zeroes sanitize to `+0.0`.
     *
     * @param cx The `e423` component.
     * @param cy The `e431` component.
     * @param cz The `e412` component.
     * @param cw The `e321` round weight; its sign picks the interior side.
     * @throws DegenerateCircleException When [cw] rounds to zero — the blade collapses to the
     *   payload `Line(cx, cy, cz)`.
     */
    public constructor(cx: Double, cy: Double, cz: Double, cw: Double) {
        if (round(cw) == 0.0) throw DegenerateCircleException(Line(cx, cy, cz))

        // Unitize: ĉ = c / round_weight_norm(c)
        val roundWeightNorm = abs(cw)
        this.cx = cx / roundWeightNorm + 0.0
        this.cy = cy / roundWeightNorm + 0.0
        this.cz = cz / roundWeightNorm + 0.0
        this.cw = cw / roundWeightNorm + 0.0
    }

    /**
     * Builds a classic circle with the given center and radius.
     *
     * A convenience over the component constructor: `cw = −1`, so the interior is the bounded
     * disk. The result is always real and classic — an imaginary or inside-out circle takes the
     * component constructor. A zero radius is the single-point circle.
     *
     * @param centerX The center's Euclidean x-coordinate.
     * @param centerY The center's Euclidean y-coordinate.
     * @param radius The radius; non-negative.
     * @throws IllegalArgumentException When [radius] is negative.
     */
    public constructor(centerX: Double, centerY: Double, radius: Double) : this(
        cx = centerX,
        cy = centerY,
        cz = -0.5 * (centerX * centerX + centerY * centerY - radius * radius),
        cw = -1.0
    ) {
        require(radius >= 0.0) { "radius must be non-negative: $radius" }
    }

    /** Adopts components as-is, skipping unitization; [raw] disambiguates from the public ctors. */
    private constructor(cx: Double, cy: Double, cz: Double, cw: Double, raw: Unit) {
        this.cx = cx
        this.cy = cy
        this.cz = cz
        this.cw = cw
    }

    /*
    PROPS
    */

    /**
     * Whether this circle is imaginary — a negative squared radius, no real points.
     *
     * `true` when [radius2] rounds below zero at the `SCALE` quantum. Imaginary circles are
     * first-class data (an imaginary dipole's [Dipole.container] is an imaginary circle),
     * never an error.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * Whether this circle is inside-out — its interior is everything outside the bounded disk.
     *
     * `true` when [cw] rounds positive at the `SCALE` quantum: the constructor stores the
     * orientation as `cw = ±1`, so this reads its sign. `-circle` flips it; [canonical]
     * normalizes to the classic orientation, where this is `false`.
     */
    public val isInsideOut: Boolean get() = round(cw) > 0.0

    /**
     * The center's distance from the origin `√(cx² + cy²)` — raw.
     *
     * The flat weight norm (also the center norm): the componentwise shortcut for the
     * distance of [center] from the origin.
     */
    public val norm: Double get() = sqrt(cx * cx + cy * cy) // flat_weight_norm / center_norm

    /**
     * The center of the circle, carrying its size — a [RoundPoint].
     *
     * The center's `aw` sits `radius2/2` above the null lift, so `center.radius2` equals this
     * circle's [radius2]. Covers both orientations uniformly and is always unitized
     * (`az = cw² = 1`; read the coordinates off `ax`/`ay`).
     */
    public val center: RoundPoint by cached {
        RoundPoint(
            ax = -cx * cw, // e1
            ay = -cy * cw, // e2
            az = cw * cw,  // e3
            aw = cx * cx + cy * cy - cz * cw // e4
        )
    }

    /**
     * The squared radius — raw and signed; negative for an imaginary circle.
     *
     * `cx² + cy² − 2·cz·cw`: exactly zero when the circle collapses to a single point, negative
     * when no real points exist. To test imaginary-ness use [isImaginary], which rounds;
     * comparing this raw value to zero is FP-fragile.
     */
    public val radius2: Double get() = cx * cx + cy * cy - 2 * cz * cw

    /**
     * The radius magnitude `√(|radius2|)` — raw, non-negative.
     *
     * For an imaginary circle this is the magnitude of the imaginary radius — gate on
     * [isImaginary] where the distinction matters.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /**
     * The purely directional part of this circle — a [Dipole] at infinity.
     *
     * The attitude `att(c) = c ∨ e412`, one antigrade up: the at-infinity [Dipole]
     * `(0, 0, −cw, −cy, cx, 0)` with `radius2 = −1`, carrying every direction of the plane at
     * once — a circle runs through them all.
     */
    public val attitude: Dipole by cached {
        Dipole(
            gz = -cw, px = -cy, py = cx,
            gx = 0.0,
            gy = 0.0,
            pz = 0.0
        )
    }

    /**
     * The carrier, saturated for a circle — the whole plane, as the antiscalar weight [cw].
     *
     * `car(c) = e4 ∧ c = cw·𝟙` is antiscalar-valued for a circle, and antiscalar results are
     * plain `Double`s.
     */
    public val carrier: Double get() = cw // AntiScalar

    /**
     * The co-carrier — the circle's center as a [FlatPoint], dropping the size.
     *
     * `ccr(c) = e4 ∧ c^☆`: the flat point `(−cx, −cy, cw)`, component-identical to
     * `center.toFlat`.
     */
    public val cocarrier: FlatPoint by cached { FlatPoint(-cx, -cy, cw) }

    /**
     * The smallest circle containing this circle — itself, orientation-normalized to
     * inside-out.
     *
     * `con(c) = cw·c`, always inside-out (`cw = +1` — what the algebra gives); preserves the
     * size: `container.radius2 == radius2`, so an imaginary circle gets an imaginary container.
     */
    public val container: Circle by cached { Circle(cw * cx, cw * cy, cw * cz, cw * cw) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [circle] — the hook the
     * shape-preserving operations return through.
     *
     * Every operation that moves a circle computes its result as a plain [Circle] and returns
     * it through this hook. This base implementation returns [circle] itself; a subclass
     * overrides it to rebuild an instance of its own class around [circle], and every such
     * operation then comes back as that class. The result's runtime class must equal the
     * receiver's — the operations check it and fail with a named error when a subclass misses
     * the override.
     *
     * @param circle The replacement geometry.
     * @return [circle] itself; an override returns its own class rebuilt around [circle].
     */
    public open fun copy(circle: Circle): Circle = circle

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this circle — useful for unoriented equivalence.
     *
     * `a.canonical == b.canonical` detects the same point set under opposed orientations. The
     * canonical orientation is the classic one ([cw] negative), negating all four components
     * when it flips; the sign decision reads rounded components, so `a == b` implies
     * `a.canonical == b.canonical`. The representative is rebuilt through [copy], so a
     * subclass comes back as its own class; an already-canonical circle returns `this`.
     */
    public open val canonical: Circle by cached {
        if (_cw > 0) copy(Circle(-cx + 0.0, -cy + 0.0, -cz + 0.0, -cw + 0.0, Unit)) else this
    }

    /**
     * Rounded, orientation-sensitive identity: equal when every component matches at the
     * `SCALE` quantum.
     *
     * A circle and its inside-out twin (negated components) trace the same point set with
     * opposite interiors and compare unequal; compare [canonical] on both sides for unoriented
     * equivalence. Identity is exact-class: a subclass instance and a plain [Circle] never
     * compare equal.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] shares this object's exact class and its four components
     *   all match after rounding.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        other as Circle
        return _cx == other._cx
                && _cy == other._cy
                && _cz == other._cz
                && _cw == other._cw
    }

    private val _hashCode by cached {
        var result = _cx.hashCode()
        result = 31 * result + _cy.hashCode()
        result = 31 * result + _cz.hashCode()
        result = 31 * result + _cw.hashCode()
        result
    }

    /** Returns the hash of the rounded components, consistent with [equals]. */
    override fun hashCode(): Int = _hashCode

    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Circle(cx=$cx, cy=$cy, cz=$cz, cw=$cw)"

    /** Returns the components rounded at the `SCALE` quantum — the debug-output convention of every primitive. */
    public fun toStringRounded(): String = "Circle(cx=$_cx, cy=$_cy, cz=$_cz, cw=$_cw)"
}

/**
 * `-Circle => Circle`<br>
 * Returns this circle turned inside out — the same point set with the interior swapped.
 *
 * Negates all four components: a classic circle comes back inside out and vice versa
 * ([Circle.isInsideOut] flips), with center and radius untouched. Identity is
 * orientation-sensitive, so a circle and its negation compare unequal; compare
 * [Circle.canonical] for unoriented equivalence — `(-circle).canonical == circle.canonical`.
 * The result is the receiver's own class: the negated geometry returns through [Circle.copy],
 * preserving a subclass that overrides the hook.
 *
 * @receiver The [Circle] to turn inside out.
 * @param T The receiver's type, preserved in the result.
 * @return The negated circle, as the receiver's class.
 */
public operator fun <T : Circle> T.unaryMinus(): T =
    narrowed(
        copy(
            Circle(
                -cx, // e423
                -cy, // e431
                -cz, // e412
                -cw, // e321
            )
        )
    )
