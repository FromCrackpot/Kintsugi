package fromcrackpot.kintsugi.cga.d2.exceptions

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundDirection
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint

/**
 * The rejection of a finite flat point where a direction is required. A position is not a free
 * vector.
 *
 * - Thrown by [FlatPoint.toDirection] when the weight doesn't round to zero at the `SCALE` quantum.
 */
public class FiniteDirectionException(
    /** The rejected finite flat point. */
    public val point: FlatPoint,
) : Exception()

/**
 * The rejection of a finite round point where a direction is required. A position is not a free
 * vector.
 *
 * - Thrown by [RoundPoint.toRoundDirection] when the round weight doesn't round to zero at the `SCALE` quantum.
 */
public class FiniteRoundDirectionException(
    /** The rejected finite round point. */
    public val point: RoundPoint,
) : Exception()
