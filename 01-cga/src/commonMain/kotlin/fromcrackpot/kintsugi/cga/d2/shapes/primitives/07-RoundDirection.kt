package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteRoundDirectionException
import kotlin.math.atan2

/**
 * A direction with magnitude — the [RoundPoint] refinement guaranteed to lie at infinity;
 * the round-point counterpart of [Direction].
 *
 * The at-infinity round point `dx·e1 + dy·e2 + aw·e4`, its round weight stored as exact
 * `0.0` — not merely a value that rounds to zero at the `SCALE` quantum. The guarantee covers
 * only the weight: the conformal component [aw] is live data — a computed at-infinity point
 * can carry meaning there (a dipole's attitude does), and the operations that move round
 * points move it honestly. [RoundPoint.toRoundDirection] converts a computed round point,
 * admitting exactly the points at infinity. The refinement adds no data, only the guarantee:
 * a round direction and a plain [RoundPoint] carrying the same components are the same value
 * and compare equal, in both directions (see [RoundPoint.equals]).
 *
 * @constructor Builds the direction `(dx, dy)` with conformal component [aw]; the zero
 *   direction is legal.
 * @param dx The `e1` component: the vector's x-extent.
 * @param dy The `e2` component: the vector's y-extent.
 * @param aw The `e4` conformal component; defaults to `0.0`.
 */
public class RoundDirection(dx: Double, dy: Double, aw: Double = 0.0) :
    RoundPoint(dx, dy, az = 0.0, aw = aw) {

    /**
     * The heading of this direction — its raw polar angle, measured counter-clockwise from
     * the positive X direction, in `(−180°, 180°]`.
     *
     * `atan2(ay, ax)`, raw at full `Double` precision and blind to the vector's length; the
     * conformal component [aw] plays no part. The negative-X heading answers `+π`, matching
     * [Angle.canonical]'s range; the zero direction answers `0.0`.
     */
    public val angle: Angle get() = Angle(atan2(ay, ax))

    /**
     * Returns this object rebuilt around the replacement geometry [roundPoint] — the hook the
     * shape-preserving operations return through, re-establishing the at-infinity guarantee.
     *
     * @param roundPoint The replacement geometry — a round point at infinity.
     * @return The replacement geometry as a round direction.
     * @throws FiniteRoundDirectionException when [roundPoint] is finite — its round weight
     *   does not round to zero at the `SCALE` quantum.
     */
    override fun copy(roundPoint: RoundPoint): RoundDirection = roundPoint.toRoundDirection
}

/**
 * `-RoundDirection => RoundDirection`<br>
 * Returns the opposite direction: the same magnitude, reversed heading.
 *
 * Component-wise negation, raw at full `Double` precision — the conformal component
 * [RoundPoint.aw] is live data and negates with the extents, so the whole at-infinity vector
 * flips. The operand carries the exact zero round weight, so the result is at infinity by
 * construction — no finite position can leak in. The zero direction is its own negation.
 *
 * @receiver The direction to negate.
 * @return The component-wise negation, as a [RoundDirection].
 */
public operator fun RoundDirection.unaryMinus(): RoundDirection = RoundDirection(-ax, -ay, -aw)
