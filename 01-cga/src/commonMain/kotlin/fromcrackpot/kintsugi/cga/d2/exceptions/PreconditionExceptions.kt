package fromcrackpot.kintsugi.cga.d2.exceptions

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.split

/**
 * The rejection of a polygon failing [Polygon.isConvex] where an operation needs a convex one.
 *
 * - Thrown by the penetration queries, the coerce verbs built on them, and [Polygon.split].
 */
public class NonConvexPolygonException(
    /** The human-readable diagnostic naming the offending polygon. */
    public val reason: String,
) : Exception()

/**
 * The rejection of an inside-out polygon where an operation needs a classic one.
 *
 * - Thrown by [Polygon.split].
 */
public class InsideOutPolygonException(
    /** The human-readable diagnostic naming the offending polygon. */
    public val reason: String,
) : Exception()

/**
 * The rejection of an operand at infinity where an operation needs a finite one. A free vector has
 * no position, and a line at infinity has no side.
 *
 * - Thrown by the distances, point differences, projections onto a line or segment, and antiprojections onto a point.
 */
public class AtInfinityException(
    /** The human-readable diagnostic naming the offending operand. */
    public val reason: String,
) : Exception()
