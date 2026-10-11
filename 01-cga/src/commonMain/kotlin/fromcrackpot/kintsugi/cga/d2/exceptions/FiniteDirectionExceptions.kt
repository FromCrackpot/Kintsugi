package fromcrackpot.kintsugi.cga.d2.exceptions

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundDirection
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint

/**
 * The rejection of a finite flat point where a direction is required — a position is not a
 * free vector.
 *
 * Thrown by [FlatPoint.toDirection] when the weight does not round to zero at the `SCALE`
 * quantum: the point is a position in the plane, and no [Direction] represents it.
 */
public class FiniteDirectionException(
    /** The rejected value — the finite flat point. */
    public val point: FlatPoint,
) : Exception()

/**
 * The rejection of a finite round point where a direction is required — a position is not a
 * free vector.
 *
 * Thrown by [RoundPoint.toRoundDirection] when the round weight does not round to zero at the
 * `SCALE` quantum: the point is a position in the plane, and no [RoundDirection] represents
 * it.
 */
public class FiniteRoundDirectionException(
    /** The rejected value — the finite round point. */
    public val point: RoundPoint,
) : Exception()
