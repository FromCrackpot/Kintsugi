package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
// Upward reach by design — the `d2/` tiers are a reading order, not a dependency DAG.
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on

/*
LINE
 */

/**
 * `Line meet Line => FlatPoint`<br>
 * Returns the point where the two lines cross — at infinity along the shared direction when
 * they are parallel.
 *
 * The antiwedge `∨`. A finite crossing re-unitizes in the [FlatPoint] constructor, so both
 * operand orders build the identical point. A parallel pair meets at infinity along the
 * lines, weighted by their separation for finite lines — there the raw blade survives, and
 * swapping the operands flips its direction. Coincident lines collapse to the all-zero flat
 * point.
 *
 * @receiver The first line.
 * @param other The second line.
 * @return The raw [FlatPoint] at the crossing; at infinity when the lines are parallel.
 */
public infix fun Line.meet(other: Line): FlatPoint = FlatPoint(
    px = this.gz * other.gy - this.gy * other.gz, // e41
    py = this.gx * other.gz - this.gz * other.gx, // e42
    pz = this.gy * other.gx - this.gx * other.gy  // e43
)

/**
 * `Line meet Circle => Dipole`<br>
 * Returns the dipole of the two points where the line crosses the circle — imaginary when
 * the line misses it.
 *
 * The antiwedge `∨`. The crossing points are the dipole's endpoints and the line is its
 * carrier, scaled by the circle's [Circle.cw]. A
 * tangent line yields a zero-radius dipole whose center is the tangent point — the
 * continuous limit of intersection, first-class, never an error — and a line that misses the
 * circle yields an imaginary dipole, first-class data. A line at infinity meets in an
 * at-infinity dipole, kept raw — gate with [Dipole.isAtInfinity]. `Circle meet Line` with
 * the operands swapped negates every component: the same crossing points, opposite
 * orientation.
 *
 * @receiver The line.
 * @param circle The circle.
 * @return The raw [Dipole] with the crossing points as endpoints; zero-radius at tangency,
 *   imaginary on a miss.
 * @throws DegenerateDipoleException When this is the all-zero line — the blade collapses.
 */
public infix fun Line.meet(circle: Circle): Dipole = Dipole(
    gx = this.gx * circle.cw, // e23
    gy = this.gy * circle.cw, // e31
    gz = this.gz * circle.cw, // e12

    px = this.gz * circle.cy - this.gy * circle.cz, // e41
    py = this.gx * circle.cz - this.gz * circle.cx, // e42
    pz = this.gy * circle.cx - this.gx * circle.cy  // e43
)

/**
 * `Line meet Dipole => RoundPoint`<br>
 * Returns the point where the line crosses the dipole's carrier.
 *
 * The antiwedge `∨`. The result is a genuine round point, not a null lift: its
 * [RoundPoint.radius2] is `r² − d²` — `r` the dipole's radius, `d` the distance from the
 * dipole's center to the crossing. Against a real dipole it is real exactly when the crossing
 * lies between the dipole's endpoints, zero-radius at an endpoint, and imaginary beyond them;
 * against an imaginary dipole (`r² < 0`) it is imaginary everywhere. Parallel carriers
 * collapse the weight: a [RoundPoint] at infinity.
 *
 * @receiver The line.
 * @param dipole The dipole whose carrier is crossed.
 * @return The raw [RoundPoint] at the crossing; at infinity when the line parallels the
 *   carrier.
 */
public infix fun Line.meet(dipole: Dipole): RoundPoint = RoundPoint(
    ax = this.gz * dipole.gy - this.gy * dipole.gz,                            // e1
    ay = this.gx * dipole.gz - this.gz * dipole.gx,                            // e2
    az = this.gy * dipole.gx - this.gx * dipole.gy,                            // e3
    aw = -(this.gx * dipole.px + this.gy * dipole.py + this.gz * dipole.pz),   // e4
)

/**
 * `Line meet Segment => RoundPoint?`<br>
 * Returns the point where the line crosses the segment, or `null` when it misses.
 *
 * The bent form of `Line meet Dipole`: the same crossing, gated to the segment itself. The
 * crossing's own realness is the bounds gate — its `radius²` is non-negative exactly when it
 * lies within the segment, the segment's endpoints included — and the gate rounds at the
 * `SCALE` quantum while the returned point stays raw. Null when the line parallels the
 * carrier, crosses it beyond the segment, or is collinear with it. Calling with
 * [Segment.toDipole] selects the ungated `Line meet Dipole` form instead.
 *
 * @receiver The line.
 * @param segment The segment.
 * @return The raw [RoundPoint] at the crossing, or `null` when there is no crossing within
 *   the segment.
 */
public infix fun Line.meet(segment: Segment): RoundPoint? =
    // `!isAtInfinity` is NOT redundant next to `!isImaginary`: parallel carriers land imaginary
    // anyway, but a collinear line zeroes every component (radius² = 0) and would slip through
    // the realness gate alone as a garbage zero point.
    (this meet segment.toDipole).takeIf { !it.isAtInfinity && !it.isImaginary }

/*
CIRCLE
 */

/**
 * `Circle meet Line => Dipole`<br>
 * Returns the dipole of the two points where the circle crosses the line — imaginary when
 * they miss.
 *
 * `Line meet Circle` with the operands swapped — but the antiwedge of two grade-3 blades is
 * antisymmetric, so this form negates every component: the same crossing points, opposite
 * orientation. Everything else matches the swapped form: zero-radius at tangency, imaginary
 * on a miss, a line at infinity meets in an at-infinity dipole.
 *
 * @receiver The circle.
 * @param line The line.
 * @return The raw [Dipole] with the crossing points as endpoints — the negation of the
 *   swapped call's result.
 * @throws DegenerateDipoleException When [line] is all-zero — the blade collapses.
 */
public infix fun Circle.meet(line: Line): Dipole = Dipole(
    gx = -this.cw * line.gx, // e23
    gy = -this.cw * line.gy, // e31
    gz = -this.cw * line.gz, // e12

    px = this.cz * line.gy - this.cy * line.gz, // e41
    py = this.cx * line.gz - this.cz * line.gx, // e42
    pz = this.cy * line.gx - this.cx * line.gy  // e43
)

/**
 * `Circle meet Circle => Dipole`<br>
 * Returns the dipole of the two points where the circles cross — imaginary when they don't
 * touch.
 *
 * The antiwedge `∨`. The dipole's carrier is the circles' radical line. Crossing circles
 * give a real dipole with the crossing points as
 * endpoints; tangent circles a zero-radius dipole centered at the tangent point; separate or
 * nested circles an imaginary dipole — first-class data encoding the separation. Concentric
 * circles meet at infinity: an at-infinity dipole, kept raw — gate with
 * [Dipole.isAtInfinity]. Swapping the operands negates every component — the same crossing
 * points, opposite orientation. Coincident circles, in either orientation, collapse the
 * blade and the unitizing [Dipole] constructor throws.
 *
 * @receiver The first circle.
 * @param other The second circle.
 * @return The raw [Dipole] with the crossing points as endpoints; zero-radius at tangency,
 *   imaginary when the circles don't touch.
 * @throws DegenerateDipoleException When the circles coincide in either orientation.
 */
public infix fun Circle.meet(other: Circle): Dipole = Dipole(
    gx = this.cx * other.cw - this.cw * other.cx, // e23
    gy = this.cy * other.cw - this.cw * other.cy, // e31
    gz = this.cz * other.cw - this.cw * other.cz, // e12

    px = this.cz * other.cy - this.cy * other.cz, // e41
    py = this.cx * other.cz - this.cz * other.cx, // e42
    pz = this.cy * other.cx - this.cx * other.cy  // e43
)

/*
SEGMENT
 */

/**
 * `Segment meet Line => RoundPoint?`<br>
 * Returns the point where the line crosses the segment, or `null` when it misses.
 *
 * `Line meet Segment` with the operands swapped. The swap is exact: the antigrade parity is
 * even, so both orders return the identical result — crossing point and `null` alike.
 *
 * @receiver The segment.
 * @param line The line.
 * @return The raw [RoundPoint] at the crossing or `null`.
 */
public infix fun Segment.meet(line: Line): RoundPoint? = line meet this

/**
 * `Segment meet Segment => RoundPoint?`<br>
 * Returns the point where the two segments cross, or `null` when they don't.
 *
 * Both segments gate the crossing, their endpoints included, and the gates round at the
 * `SCALE` quantum while the returned point stays raw. Null when the carriers are parallel,
 * when the crossing lies beyond either segment, or when the segments are collinear — a
 * collinear overlap is not a point.
 *
 * @receiver The first segment.
 * @param other The second segment.
 * @return The raw [RoundPoint] at the crossing, or `null` when the segments don't cross.
 */
public infix fun Segment.meet(other: Segment): RoundPoint? =
    // The meet's realness encodes bounds wrt `other`'s dipole only, so the receiver's side
    // needs its own `on` probe.
    (this.carrier meet other)?.takeIf { it on this }
