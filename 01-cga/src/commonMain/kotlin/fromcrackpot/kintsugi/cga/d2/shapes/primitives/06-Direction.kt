package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteDirectionException
import kotlin.math.atan2

/**
 * A free vector, with magnitude and orientation but no position. It is the [FlatPoint] refinement
 * guaranteed to lie at infinity.
 *
 * - Its weight is exact `0.0`, so no operation can leak a position back in.
 * - Equals a plain [FlatPoint] with the same components. [FlatPoint.toDirection] converts one.
 *
 * @constructor Builds the free vector `(dx, dy)`. The zero direction is legal.
 * @param dx The `e41` component: the vector's x-extent.
 * @param dy The `e42` component: the vector's y-extent.
 */
public class Direction(dx: Double, dy: Double) : FlatPoint(dx, dy, pz = 0.0) {

    /**
     * The raw polar angle of this free vector from the positive X direction, in `(−180°, 180°]`.
     * The zero direction answers `0`.
     */
    public val angle: Angle get() = Angle(atan2(py, px))

    /**
     * Returns this object rebuilt around the replacement geometry [flatPoint], keeping the
     * at-infinity guarantee.
     *
     * @param flatPoint The replacement geometry, a flat point at infinity.
     * @return The replacement geometry as a direction.
     * @throws FiniteDirectionException when [flatPoint] is finite.
     */
    override fun copy(flatPoint: FlatPoint): Direction = flatPoint.toDirection
}

/**
 * `Direction + Direction => Direction`<br>
 * Returns the raw vector sum of the two free vectors, applying both displacements together.
 *
 * @receiver The left direction.
 * @param other The direction added to this one.
 * @return The sum.
 */
public operator fun Direction.plus(other: Direction): Direction =
    Direction(px + other.px, py + other.py)

/**
 * `Direction - Direction => Direction`<br>
 * Returns the raw vector difference of the two free vectors. It is this displacement with [other] undone.
 *
 * @receiver The left direction.
 * @param other The direction subtracted from this one.
 * @return The difference.
 */
public operator fun Direction.minus(other: Direction): Direction =
    Direction(px - other.px, py - other.py)

/**
 * `-Direction => Direction`<br>
 * Returns the opposite free vector, with the same length and the reversed heading.
 *
 * @receiver The direction to negate.
 * @return The negated direction.
 */
public operator fun Direction.unaryMinus(): Direction = Direction(-px, -py)
