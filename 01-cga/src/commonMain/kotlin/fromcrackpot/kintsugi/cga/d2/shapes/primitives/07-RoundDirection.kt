package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteRoundDirectionException
import kotlin.math.atan2

/**
 * A direction with magnitude. It is the [RoundPoint] refinement guaranteed to lie at infinity, and
 * the round counterpart of [Direction].
 *
 * - Its round weight is exact `0.0`. Its [aw][RoundDirection.aw] stays live data that operations move.
 * - Equals a plain [RoundPoint] with the same components. [RoundPoint.toRoundDirection] converts one.
 *
 * @constructor Builds the direction `(dx, dy)` with conformal component `aw`. The zero direction
 *   is legal.
 * @param dx The `e1` component: the vector's x-extent.
 * @param dy The `e2` component: the vector's y-extent.
 * @param aw The `e4` conformal component, `0.0` by default.
 */
public class RoundDirection(dx: Double, dy: Double, aw: Double = 0.0) :
    RoundPoint(dx, dy, az = 0.0, aw = aw) {

    /**
     * The raw polar angle of this direction from the positive X direction, in `(−180°, 180°]`.
     * The zero direction answers `0`.
     */
    public val angle: Angle get() = Angle(atan2(ay, ax))

    /**
     * Returns this object rebuilt around the replacement geometry [roundPoint], keeping the
     * at-infinity guarantee.
     *
     * @param roundPoint The replacement geometry, a round point at infinity.
     * @return The replacement geometry as a round direction.
     * @throws FiniteRoundDirectionException when [roundPoint] is finite.
     */
    override fun copy(roundPoint: RoundPoint): RoundDirection = roundPoint.toRoundDirection
}

/**
 * `-RoundDirection => RoundDirection`<br>
 * Returns the opposite direction, with the same magnitude and the reversed heading. [RoundPoint.aw]
 * is negated too.
 *
 * @receiver The direction to negate.
 * @return The negated direction.
 */
public operator fun RoundDirection.unaryMinus(): RoundDirection = RoundDirection(-ax, -ay, -aw)
