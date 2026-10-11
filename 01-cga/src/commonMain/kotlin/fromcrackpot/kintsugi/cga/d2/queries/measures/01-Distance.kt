package fromcrackpot.kintsugi.cga.d2.queries.measures

import fromcrackpot.kintsugi.cga.d2.products.dot
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
 * The measurement `‖att(a ∧ b)‖●` of the round representations: the join of the operands is
 * the dipole through both points, its attitude the separation as a direction at infinity, and
 * that direction's round-bulk magnitude the distance — componentwise, the coordinate
 * differences, which the body evaluates directly. The result is non-negative and raw at full
 * `Double` precision. A point at infinity — zero weight — carries a direction rather than a
 * position, and the result is no distance; gate with [FlatPoint.isAtInfinity].
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw non-negative distance between the points.
 */
public infix fun FlatPoint.distanceTo(other: FlatPoint): Double {
    // ‖att(a ∧ b)‖● reduced to the difference form — the attitude's
    // round bulk is (dx, dy); evaluating on differences keeps the error scaling with the
    // separation, not the coordinate magnitude.
    val dx = other.px - this.px
    val dy = other.py - this.py
    return sqrt(dx * dx + dy * dy)
}

/**
 * `FlatPoint distanceTo RoundPoint => Double`<br>
 * Returns the raw Euclidean distance between the flat point and the round point's position.
 *
 * The round point contributes its position via [RoundPoint.toFlat] — the flat weight copies
 * the round weight, so a point at infinity stays at infinity — and the measurement is
 * `FlatPoint distanceTo FlatPoint`. A sized or imaginary round point measures from its
 * position all the same: the conformal component takes no part.
 *
 * @receiver The flat point.
 * @param point The round point contributing its position.
 * @return The raw non-negative distance between the positions.
 */
public infix fun FlatPoint.distanceTo(point: RoundPoint): Double = this distanceTo point.toFlat

/**
 * `FlatPoint distanceTo Line => Double`<br>
 * Returns the raw perpendicular distance from the point to the line.
 *
 * The magnitude of the meet of the round representations — for a finite point and a finite
 * line, the line's equation evaluated at the point, `|px·gx + py·gy + pz·gz|`. The sign
 * discarded here is the point's sidedness; the signed reading is `FlatPoint dot Line`. A
 * point at infinity pairs its direction against the line's normal instead, and a line at
 * infinity — zero normal — pairs only weights: neither result is a distance; gate with
 * [FlatPoint.isAtInfinity] and [Line.isAtInfinity].
 *
 * @receiver The point measured from.
 * @param line The line measured to.
 * @return The raw non-negative perpendicular distance.
 */
public infix fun FlatPoint.distanceTo(line: Line): Double = abs(this dot line)

/*
ROUND POINT
 */

/**
 * `RoundPoint distanceTo RoundPoint => Double`<br>
 * Returns the raw Euclidean distance between the two points' positions.
 *
 * The measurement `‖att(a ∧ b)‖●`: the join of the operands is the dipole through both
 * points, its attitude the separation as a direction at infinity, and that direction's
 * round-bulk magnitude the distance — componentwise, the coordinate differences, which the
 * body evaluates directly. The result is non-negative and raw at full `Double` precision. A
 * sized or imaginary round point measures from its position: the conformal component takes
 * no part. A point at infinity — zero weight — carries a direction rather than a position,
 * and the result is no distance; gate with [RoundPoint.isAtInfinity].
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw non-negative distance between the positions.
 */
public infix fun RoundPoint.distanceTo(other: RoundPoint): Double {
    // ‖att(a ∧ b)‖● reduced to the difference form — the attitude's
    // round bulk is (dx, dy); evaluating on differences keeps the error scaling with the
    // separation, not the coordinate magnitude.
    val dx = other.ax - this.ax
    val dy = other.ay - this.ay
    return sqrt(dx * dx + dy * dy)
}

/**
 * `RoundPoint distanceTo FlatPoint => Double`<br>
 * Returns the raw Euclidean distance between the round point's position and the flat point.
 *
 * This point contributes its position via [RoundPoint.toFlat] — the flat weight copies the
 * round weight, so a point at infinity stays at infinity — and the measurement is
 * `FlatPoint distanceTo FlatPoint`. A sized or imaginary round point measures from its
 * position all the same: the conformal component takes no part.
 *
 * @receiver The round point contributing its position.
 * @param flatPoint The flat point.
 * @return The raw non-negative distance between the positions.
 */
public infix fun RoundPoint.distanceTo(flatPoint: FlatPoint): Double = this.toFlat distanceTo flatPoint

/**
 * `RoundPoint distanceTo Line => Double`<br>
 * Returns the raw perpendicular distance from the point's position to the line.
 *
 * The magnitude of the meet of the operands — for a finite point and a finite line, the
 * line's equation evaluated at the point, `|ax·gx + ay·gy + az·gz|`; the conformal component
 * takes no part, so a sized or imaginary round point measures from its position. The sign
 * discarded here is the point's sidedness; the signed reading is `RoundPoint dot Line`. A
 * point at infinity pairs its direction against the line's normal instead, and a line at
 * infinity — zero normal — pairs only weights: neither result is a distance; gate with
 * [RoundPoint.isAtInfinity] and [Line.isAtInfinity].
 *
 * @receiver The point measured from.
 * @param line The line measured to.
 * @return The raw non-negative perpendicular distance.
 */
public infix fun RoundPoint.distanceTo(line: Line): Double = abs(this dot line)
