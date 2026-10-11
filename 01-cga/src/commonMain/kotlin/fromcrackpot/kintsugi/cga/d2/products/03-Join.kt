package fromcrackpot.kintsugi.cga.d2.products

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
 * The projective cross product of the two `(px, py, pz)` triples. The strict CGA wedge of two
 * flat points is zero — both carry a factor of `e4` — so this is the family's one non-blade
 * pairing, ported from the RGA join because the line through the two points is what joining
 * them means. Joining a finite point with a direction (at infinity) gives the line through
 * the point running along it. Coincident points collapse to the all-zero line, which the
 * [Line] constructor accepts.
 *
 * @receiver The first point.
 * @param other The second point.
 * @return The raw [Line] through both points; all-zero when they coincide.
 */
public infix fun FlatPoint.join(other: FlatPoint): Line = Line(
    gx = this.py * other.pz - this.pz * other.py, // e423
    gy = this.pz * other.px - this.px * other.pz, // e431
    gz = this.px * other.py - this.py * other.px  // e412
)

/**
 * `FlatPoint join RoundPoint => Line`<br>
 * Returns the line through the flat point and the round point.
 *
 * A strict wedge. The pairing produces no `e321` component — every flat-point blade carries
 * `e4` — so a flat and a round point always join into a line, never a circle. The normal
 * points to the left of the travel from the flat point to the round point, in either operand
 * order. An operand at infinity gives the line through the finite operand running along the
 * carried direction; coincident operands collapse to the all-zero line, which the [Line]
 * constructor accepts.
 *
 * @receiver The flat point.
 * @param point The round point.
 * @return The raw [Line] through both operands; all-zero when they coincide.
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
 * Returns the line through both points.
 *
 * `FlatPoint join RoundPoint` with the operands swapped. The swap is exact: the
 * grade-1·grade-2 wedge parity is even, so both orders build the identical [Line],
 * orientation included — the normal sits to the left of the flat→round travel in either
 * spelling.
 *
 * @receiver The round point.
 * @param flatPoint The flat point.
 * @return The raw [Line] through both operands.
 */
public infix fun RoundPoint.join(flatPoint: FlatPoint): Line = flatPoint join this

/**
 * `RoundPoint join RoundPoint => Dipole`<br>
 * Returns the dipole holding both points — this point as [Dipole.start], [other] as
 * [Dipole.end].
 *
 * A strict wedge. The operand order is the dipole's orientation: `start join end` rebuilds it
 * exactly, and swapping the operands negates every component. At-infinity operands join into
 * an at-infinity dipole, kept raw — gate with [Dipole.isAtInfinity] — unless their directions
 * are parallel: then the wedge collapses just as coincident points do, and the unitizing
 * [Dipole] constructor throws.
 *
 * @receiver The first point, the result's [Dipole.start].
 * @param other The second point, the result's [Dipole.end].
 * @return The raw [Dipole] with both points.
 * @throws DegenerateDipoleException When the wedge collapses at the `SCALE` quantum — the
 *   points coincide, or two at-infinity operands carry parallel directions.
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
 * `Dipole join RoundPoint` with the operands swapped. The swap is exact: the grade-1·grade-2
 * wedge parity is even, so both orders build the identical [Circle], orientation included.
 *
 * @receiver The point off the dipole.
 * @param dipole The dipole contributing its endpoints.
 * @return The raw [Circle] through the point and the dipole's endpoints.
 * @throws DegenerateCircleException When this point lies on the dipole's carrier at the
 *   `SCALE` quantum; carries the [Line] through the collinear points.
 */
public infix fun RoundPoint.join(dipole: Dipole): Circle = dipole join this

/**
 * `RoundPoint join Segment => Circle`<br>
 * Returns the circle through the segment's endpoints and this point.
 *
 * `Segment join RoundPoint` with the operands swapped. The swap is exact: the underlying
 * grade-1·grade-2 wedge parity is even, so both orders build the identical [Circle],
 * orientation included.
 *
 * @receiver The point off the segment.
 * @param segment The segment contributing its endpoints.
 * @return The raw [Circle] through the segment's endpoints and this point.
 * @throws DegenerateCircleException When this point is collinear with the segment at the
 *   `SCALE` quantum; carries the [Line] through the collinear points.
 */
public infix fun RoundPoint.join(segment: Segment): Circle = segment join this

/*
DIPOLE
 */

/**
 * `Dipole join RoundPoint => Circle`<br>
 * Returns the circle through the dipole's endpoints and [point].
 *
 * A strict wedge. For a finite real dipole the walk [Dipole.start] → [Dipole.end] → [point]
 * decides the orientation: a counterclockwise turn builds a classic circle, a clockwise turn
 * an inside-out one. A point on the dipole's carrier collapses the round weight — three
 * collinear points trace a line, not a circle — and the unitizing [Circle] constructor
 * throws, carrying that line.
 *
 * @receiver The dipole contributing its endpoints.
 * @param point The point off the dipole.
 * @return The raw [Circle] through the dipole's endpoints and [point].
 * @throws DegenerateCircleException When [point] lies on the dipole's carrier at the `SCALE`
 *   quantum; carries the [Line] through the collinear points.
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
 * Delegates to `Dipole join RoundPoint` via [Segment.toDipole] — a circle through a
 * segment's endpoints plus a third point is just a circle, so the composite form changes
 * nothing; it keeps [Segment] first-class at call sites. Orientation and degeneracy follow
 * the dipole form.
 *
 * @receiver The segment contributing its endpoints.
 * @param point The point off the segment.
 * @return The raw [Circle] through the segment's endpoints and [point].
 * @throws DegenerateCircleException When [point] is collinear with the segment at the `SCALE`
 *   quantum; carries the [Line] through the collinear points.
 */
public infix fun Segment.join(point: RoundPoint): Circle = this.toDipole join point
