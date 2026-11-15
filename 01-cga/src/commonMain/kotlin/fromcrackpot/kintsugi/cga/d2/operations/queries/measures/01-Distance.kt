package fromcrackpot.kintsugi.cga.d2.operations.queries.measures

import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import kotlin.math.abs
import kotlin.math.sqrt


/*
FLAT POINT
 */

/**
 * `FlatPoint distanceTo FlatPoint => Double`<br>
 * Returns the raw Euclidean distance between the two points.
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when either point lies at infinity.
 */
public infix fun FlatPoint.distanceTo(other: FlatPoint): Double {
    if (isAtInfinity || other.isAtInfinity) {
        throw AtInfinityException("no distance to a point at infinity: $this, $other")
    }
    // ‖att(a ∧ b)‖● in difference form: the attitude's round bulk is (dx, dy).
    // Differences keep the error scaling with the separation, not the coordinate magnitude.
    val dx = other.px - this.px
    val dy = other.py - this.py
    return sqrt(dx * dx + dy * dy)
}

/**
 * `FlatPoint distanceTo RoundPoint => Double`<br>
 * Returns the raw Euclidean distance between the flat point and the round point's position.
 *
 * - A sized or imaginary round point measures from its position.
 *
 * @receiver The flat point.
 * @param point The round point contributing its position.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when either point lies at infinity.
 */
public infix fun FlatPoint.distanceTo(point: RoundPoint): Double = this distanceTo point.toFlat

/**
 * `FlatPoint distanceTo Line => Double`<br>
 * Returns the raw perpendicular distance from the point to the line.
 *
 * - Unsigned: the signed reading is `FlatPoint dot Line`.
 *
 * @receiver The point measured from.
 * @param line The line measured to.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when the point or the line lies at infinity.
 */
public infix fun FlatPoint.distanceTo(line: Line): Double {
    if (isAtInfinity || line.isAtInfinity) {
        throw AtInfinityException("no distance with an operand at infinity: $this, $line")
    }
    return abs(this dot line)
}

/*
ROUND POINT
 */

/**
 * `RoundPoint distanceTo RoundPoint => Double`<br>
 * Returns the raw Euclidean distance between the two points' positions.
 *
 * - A sized or imaginary round point measures from its position.
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when either point lies at infinity.
 */
public infix fun RoundPoint.distanceTo(other: RoundPoint): Double {
    if (isAtInfinity || other.isAtInfinity) {
        throw AtInfinityException("no distance to a point at infinity: $this, $other")
    }
    // ‖att(a ∧ b)‖● in difference form: the attitude's round bulk is (dx, dy).
    // Differences keep the error scaling with the separation, not the coordinate magnitude.
    val dx = other.ax - this.ax
    val dy = other.ay - this.ay
    return sqrt(dx * dx + dy * dy)
}

/**
 * `RoundPoint distanceTo FlatPoint => Double`<br>
 * Returns the raw Euclidean distance between the round point's position and the flat point.
 *
 * - A sized or imaginary round point measures from its position.
 *
 * @receiver The round point contributing its position.
 * @param flatPoint The flat point.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when either point lies at infinity.
 */
public infix fun RoundPoint.distanceTo(flatPoint: FlatPoint): Double = this.toFlat distanceTo flatPoint

/**
 * `RoundPoint distanceTo Line => Double`<br>
 * Returns the raw perpendicular distance from the point's position to the line.
 *
 * - Unsigned: the signed reading is `RoundPoint dot Line`.
 *
 * @receiver The point measured from.
 * @param line The line measured to.
 * @return The raw non-negative distance.
 * @throws AtInfinityException when the point or the line lies at infinity.
 */
public infix fun RoundPoint.distanceTo(line: Line): Double {
    if (isAtInfinity || line.isAtInfinity) {
        throw AtInfinityException("no distance with an operand at infinity: $this, $line")
    }
    return abs(this dot line)
}
