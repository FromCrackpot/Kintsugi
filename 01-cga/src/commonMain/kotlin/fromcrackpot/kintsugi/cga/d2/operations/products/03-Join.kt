package fromcrackpot.kintsugi.cga.d2.operations.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException

/*
FLAT POINT
 */

/**
 * `FlatPoint join FlatPoint => Line`<br>
 * Returns the line through both points, oriented so its normal points to the left of the
 * travel from this point to [other].
 *
 * - Coincident points give the all-zero line, not an error.
 * - A direction (at infinity) gives the line through the finite point running along it.
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw line through both points.
 */
public infix fun FlatPoint.join(other: FlatPoint): Line = Line(
    gx = this.py * other.pz - this.pz * other.py, // e423
    gy = this.pz * other.px - this.px * other.pz, // e431
    gz = this.px * other.py - this.py * other.px  // e412
)

/**
 * `FlatPoint join RoundPoint => Line`<br>
 * Returns the line through the flat point and the round point. Its normal points to the left of
 * the travel from the flat point to the round point.
 *
 * - Coincident operands give the all-zero line, not an error.
 *
 * @receiver The flat point.
 * @param point The round point.
 * @return The raw line through both operands.
 */
public infix fun FlatPoint.join(point: RoundPoint): Line = Line(
    gx = this.py * point.az - this.pz * point.ay, // e423
    gy = this.pz * point.ax - this.px * point.az, // e431
    gz = this.px * point.ay - this.py * point.ax, // e412
)

/*
ROUND POINT
 */

/**
 * `RoundPoint join FlatPoint => Line`<br>
 * Returns the line through both points. Its normal points to the left of the travel from the flat
 * point to the round point.
 *
 * @receiver The round point.
 * @param flatPoint The flat point.
 * @return The raw line through both operands.
 */
public infix fun RoundPoint.join(flatPoint: FlatPoint): Line = flatPoint join this

/**
 * `RoundPoint join RoundPoint => Dipole`<br>
 * Returns the dipole holding both points, with this point as [Dipole.start] and [other] as
 * [Dipole.end].
 *
 * - Swapping the operands negates every component.
 * - At-infinity operands join into a raw dipole at infinity. Check [Dipole.isAtInfinity].
 *
 * @receiver The first point, the result's start.
 * @param other The second point, the result's end.
 * @return The raw dipole.
 * @throws DegenerateDipoleException when the points coincide at the `SCALE` quantum, or two
 *   at-infinity operands carry parallel directions.
 */
public infix fun RoundPoint.join(other: RoundPoint): Dipole = Dipole(
    gx = this.ay * other.az - this.az * other.ay,   // e23
    gy = this.az * other.ax - this.ax * other.az,   // e31
    gz = this.ax * other.ay - this.ay * other.ax,   // e12

    px = this.aw * other.ax - this.ax * other.aw,   // e41
    py = this.aw * other.ay - this.ay * other.aw,   // e42
    pz = this.aw * other.az - this.az * other.aw,   // e43
)

/**
 * `RoundPoint join Dipole => Circle`<br>
 * Returns the circle through the point and the dipole's endpoints.
 *
 * @receiver The point off the dipole.
 * @param dipole The dipole contributing its endpoints.
 * @return The raw circle.
 * @throws DegenerateCircleException when this point lies on the dipole's carrier at the `SCALE`
 *   quantum. It carries the line through the collinear points.
 */
public infix fun RoundPoint.join(dipole: Dipole): Circle = dipole join this

/**
 * `RoundPoint join Segment => Circle`<br>
 * Returns the circle through the segment's endpoints and this point.
 *
 * @receiver The point off the segment.
 * @param segment The segment contributing its endpoints.
 * @return The raw circle.
 * @throws DegenerateCircleException when this point is collinear with the segment at the `SCALE`
 *   quantum. It carries the line through the collinear points.
 */
public infix fun RoundPoint.join(segment: Segment): Circle = segment join this

/*
DIPOLE
 */

/**
 * `Dipole join RoundPoint => Circle`<br>
 * Returns the circle through the dipole's endpoints and [point].
 *
 * - The walk start → end → [point] decides orientation: counter-clockwise classic, clockwise inside-out.
 *
 * @receiver The dipole contributing its endpoints.
 * @param point The point off the dipole.
 * @return The raw circle.
 * @throws DegenerateCircleException when [point] lies on the dipole's carrier at the `SCALE`
 *   quantum. It carries the line through the collinear points.
 */
public infix fun Dipole.join(point: RoundPoint): Circle = Circle(
    cx = this.py * point.az - this.pz * point.ay + this.gx * point.aw,   // e423
    cy = this.pz * point.ax - this.px * point.az + this.gy * point.aw,   // e431
    cz = this.px * point.ay - this.py * point.ax + this.gz * point.aw,   // e412
    cw = -(this.gx * point.ax + this.gy * point.ay + this.gz * point.az) // e321
)

/*
SEGMENT
 */

/**
 * `Segment join RoundPoint => Circle`<br>
 * Returns the circle through the segment's endpoints and [point].
 *
 * - The walk start → end → [point] decides orientation: counter-clockwise classic, clockwise inside-out.
 *
 * @receiver The segment contributing its endpoints.
 * @param point The point off the segment.
 * @return The raw circle.
 * @throws DegenerateCircleException when [point] is collinear with the segment at the `SCALE`
 *   quantum. It carries the line through the collinear points.
 */
public infix fun Segment.join(point: RoundPoint): Circle = this.toDipole join point
