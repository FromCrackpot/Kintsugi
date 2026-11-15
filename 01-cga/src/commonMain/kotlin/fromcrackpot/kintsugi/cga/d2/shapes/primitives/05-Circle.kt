package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.cached
import fromcrackpot.kintsugi.cga.narrowed
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * A circle in the plane, oriented by which side counts as its interior.
 *
 * - Classic (`cw = −1`): the interior is the bounded disk; inside-out (`cw = +1`): everything outside.
 * - A negative [radius2] makes it imaginary. It has no real points but is valid data, not an error.
 * - Orientation is data, so `-circle` compares unequal. Use [canonical] for unoriented equivalence.
 */
public open class Circle : Primitive {
    /** The `e423` component: the center's x-coordinate for a classic circle; negated when inside-out. */
    public val cx: Double // e423

    /** The `e431` component: the center's y-coordinate for a classic circle; negated when inside-out. */
    public val cy: Double // e431

    /**
     * The `e412` component: minus half the origin's power with respect to the circle,
     * `(radius² − ‖center‖²)/2`, for a classic circle; negated when inside-out.
     */
    public val cz: Double // e412

    /**
     * The `e321` round weight: `−1` for a classic circle, whose interior is the bounded disk; `+1`
     * for an inside-out one, whose interior is everything outside it.
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
     * Builds the circle `(cx, cy, cz, cw)`, unitized, with `-0.0` cleared to `0.0`. The sign of [cw]
     * survives as the orientation.
     *
     * @param cx The `e423` component.
     * @param cy The `e431` component.
     * @param cz The `e412` component.
     * @param cw The `e321` round weight. Its sign picks the interior side.
     * @throws DegenerateCircleException when [cw] rounds to zero at the `SCALE` quantum. The blade is
     *   then the line `(cx, cy, cz)`.
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
     * Builds the classic circle with the given center and radius. It is always real, and its
     * interior is the bounded disk.
     *
     * @param centerX The center's Euclidean x-coordinate.
     * @param centerY The center's Euclidean y-coordinate.
     * @param radius The radius. Zero gives the single-point circle.
     * @throws IllegalArgumentException when [radius] is negative.
     */
    public constructor(centerX: Double, centerY: Double, radius: Double) : this(
        cx = centerX,
        cy = centerY,
        cz = -0.5 * (centerX * centerX + centerY * centerY - radius * radius),
        cw = -1.0
    ) {
        require(radius >= 0.0) { "radius must be non-negative: $radius" }
    }

    /** Adopts the components as-is, skipping unitization. [raw] only tells this ctor apart from the public ones. */
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
     * Whether this circle is imaginary. True when [radius2] rounds below zero at the `SCALE` quantum.
     *
     * - An imaginary circle is valid data, not an error.
     */
    public val isImaginary: Boolean get() = round(radius2) < 0.0

    /**
     * Whether this circle is inside-out, with everything outside the disk as its interior. True when
     * [cw] rounds positive.
     */
    public val isInsideOut: Boolean get() = round(cw) > 0.0

    /** The raw distance of [center] from the origin, `√(cx² + cy²)`. */
    public val norm: Double get() = sqrt(cx * cx + cy * cy) // flat_weight_norm / center_norm

    /**
     * The center of the circle. It carries the circle's size, so `center.radius2` equals [radius2].
     *
     * - Use `center.toFlat` (or read `ax`/`ay`) for the bare position.
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
     * The raw, signed squared radius: zero for a single-point circle, negative for an imaginary one.
     *
     * - Test imaginary-ness with [isImaginary], which rounds. This raw value is unstable near zero.
     */
    public val radius2: Double get() = cx * cx + cy * cy - 2 * cz * cw

    /**
     * The raw radius magnitude `√(|radius2|)`, never negative. Check [isImaginary] where the sign
     * matters.
     */
    public val radius: Double get() = sqrt(radius2.absoluteValue)

    /**
     * The purely directional part of this circle, as a [Dipole] at infinity that carries every
     * direction of the plane.
     */
    public val attitude: Dipole by cached {
        Dipole(
            gz = -cw, px = -cy, py = cx,
            gx = 0.0,
            gy = 0.0,
            pz = 0.0
        )
    }

    /** The carrier, saturated for a circle. It is the whole plane, as the weight [cw]. */
    public val carrier: Double get() = cw // AntiScalar

    /**
     * The circle's center as a [FlatPoint], without the size. It is component-identical to
     * `center.toFlat`.
     */
    public val cocarrier: FlatPoint by cached { FlatPoint(-cx, -cy, cw) }

    /**
     * The smallest circle containing this one, which is itself made inside-out.
     *
     * - An imaginary circle gets an imaginary container.
     */
    public val container: Circle by cached { Circle(cw * cx, cw * cy, cw * cz, cw * cw) }

    /*
    COPY
     */

    /**
     * Returns this object rebuilt around the replacement geometry [circle]. Shape-preserving
     * operations return through this hook.
     *
     * - Override it to rebuild your subclass. Every generic operation then returns that class.
     * - The result's runtime class must equal the receiver's, or the operations fail with a named error.
     *
     * @param circle The replacement geometry.
     * @return [circle] itself. An override returns its own class rebuilt around [circle].
     */
    public open fun copy(circle: Circle): Circle = circle

    /*
    IDENTITY
    */

    /**
     * The canonical representative of this circle, in the classic orientation. Both orientations
     * share it.
     */
    public open val canonical: Circle by cached {
        if (_cw > 0) copy(Circle(-cx + 0.0, -cy + 0.0, -cz + 0.0, -cw + 0.0, Unit)) else this
    }

    /**
     * Compares the four components rounded at the `SCALE` quantum.
     *
     * - An inside-out twin compares unequal. Compare [canonical] on both sides for unoriented equivalence.
     * - Classes must match exactly. A subclass instance never equals a plain [Circle].
     *
     * @param other The object to compare against.
     * @return `true` when [other] has this exact class and every rounded component matches.
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

    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Circle(cx=$cx, cy=$cy, cz=$cz, cw=$cw)"

    /** Returns the components rounded at the `SCALE` quantum, for debug output. */
    public fun toStringRounded(): String = "Circle(cx=$_cx, cy=$_cy, cz=$_cz, cw=$_cw)"
}

/**
 * `-Circle => Circle`<br>
 * Returns this circle turned inside out. The point set stays, the interior swaps.
 *
 * - Compares unequal to the original, but `(-circle).canonical == circle.canonical`.
 *
 * @receiver The circle to turn inside out.
 * @param T The receiver's type, preserved in the result.
 * @return The negated circle.
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
