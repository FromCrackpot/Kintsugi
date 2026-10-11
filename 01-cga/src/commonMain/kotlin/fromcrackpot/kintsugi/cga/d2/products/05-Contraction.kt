package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*


/*
FLAT POINT
 */

/**
 * `FlatPoint contract RoundPoint => RoundPoint`<br>
 * Returns the flat point's position as a round point sized by minus the squared distance to
 * the given point.
 *
 * The bulk contraction `p ∨ point^★` — the round point contained in the flat point and
 * orthogonal to the round one: the flat point's own position, whose `radius2` is `−d²` for
 * unitized operands with a null-lift argument — zero when they coincide, imaginary apart. A
 * flat point at infinity contracts to a point at infinity, kept raw.
 *
 * @receiver The flat point contributing its position.
 * @param point The point contracted away.
 * @return The raw [RoundPoint] at the flat point's position; imaginary when the operands
 *   sit apart.
 */
public infix fun FlatPoint.contract(point: RoundPoint): RoundPoint = RoundPoint(
    ax = -point.az * this.px,                                               // e1
    ay = -point.az * this.py,                                               // e2
    az = -point.az * this.pz,                                               // e3
    aw = point.aw * this.pz - point.ax * this.px - point.ay * this.py       // e4
)

/*
LINE
 */

/**
 * `Line contract RoundPoint => Dipole`<br>
 * Returns the dipole on the line centered at the point's perpendicular foot, its squared
 * radius minus the squared distance to the line.
 *
 * The bulk contraction `g ∨ point^★` — the dipole contained in the line and orthogonal to
 * the point. A point on the line contracts to a zero-radius dipole at itself; any point off
 * it gives an imaginary dipole (`radius2 = −d²` for unitized operands, `d` the distance).
 * The result's carrier runs opposite this line's orientation. A line at infinity contracts
 * into an at-infinity dipole, kept raw — gate with [Dipole.isAtInfinity].
 *
 * @receiver The line carrying the result.
 * @param point The point contracted away.
 * @return The raw [Dipole] on the line centered at the point's foot; imaginary off the line.
 * @throws DegenerateDipoleException When the point lies at infinity at the `SCALE` quantum —
 *   the whole blade collapses to its flat part.
 */
public infix fun Line.contract(point: RoundPoint): Dipole = Dipole(
    gx = -point.az * this.gx,                               // e23
    gy = -point.az * this.gy,                               // e31
    gz = -point.az * this.gz,                               // e12

    px = point.aw * this.gy + point.ay * this.gz,           // e41
    py = -(point.aw * this.gx + point.ax * this.gz),        // e42
    pz = point.ax * this.gy - point.ay * this.gx            // e43
)

/**
 * `Line contract FlatPoint => RoundPoint`<br>
 * Returns the line's running direction as a point at infinity, weighted by the flat point.
 *
 * The bulk contraction `g ∨ flatPoint^★` — a purely directional object contained in the line
 * and orthogonal to the flat point: the direction the line runs, scaled by the flat point's
 * weight, with the raw conformal component carrying the operands' pairing. The result always
 * lies at infinity and is kept raw — no unitization applies.
 *
 * @receiver The line contributing its direction.
 * @param flatPoint The point contracted away.
 * @return The raw at-infinity [RoundPoint] along the line's direction.
 */
public infix fun Line.contract(flatPoint: FlatPoint): RoundPoint = RoundPoint(
    ax = -this.gy * flatPoint.pz,                           // e1
    ay = this.gx * flatPoint.pz,                            // e2
    az = 0.0,                                               // e3
    aw = this.gx * flatPoint.py - this.gy * flatPoint.px    // e4
)

/**
 * `Line contract Dipole => RoundPoint`<br>
 * Returns the point where the line crosses the dipole's perpendicular bisector.
 *
 * The bulk contraction `g ∨ dipole^★` — the round point contained in the line and orthogonal
 * to the dipole. The result is a genuine round point, not a null lift: its position is the
 * crossing with the endpoints' perpendicular bisector and its raw conformal component sizes
 * it by the pairing. A line parallel to that bisector — the dipole's carrier perpendicular
 * to it — contracts to a point at infinity, kept raw.
 *
 * @receiver The line carrying the result.
 * @param dipole The dipole contracted away.
 * @return The raw [RoundPoint] at the crossing; at infinity when the line parallels the
 *   endpoints' perpendicular bisector.
 */
public infix fun Line.contract(dipole: Dipole): RoundPoint = RoundPoint(
    ax = -(dipole.gx * this.gz + dipole.pz * this.gy),                      // e1
    ay = dipole.pz * this.gx - dipole.gy * this.gz,                         // e2
    az = dipole.gx * this.gx + dipole.gy * this.gy,                         // e3
    aw = dipole.gz * this.gz - dipole.px * this.gy + dipole.py * this.gx    // e4
)

/*
DIPOLE
 */

/**
 * `Dipole contract RoundPoint => RoundPoint`<br>
 * Returns the center of the circle that swaps the dipole's endpoints through the point,
 * carrying that circle's squared radius negated.
 *
 * The bulk contraction `d ∨ point^★` — the round point contained in the dipole and
 * orthogonal to the point: componentwise the antidual of the swap-circle
 * `RoundPoint expand Dipole`, so its position is that circle's center and its `radius2` the
 * circle's negated. A point on the endpoints' perpendicular bisector — where the swap-circle
 * straightens into a line — contracts to a point at infinity, kept raw.
 *
 * @receiver The dipole carrying the result.
 * @param point The point contracted away.
 * @return The raw [RoundPoint] at the swap-circle's center; at infinity when the point lies
 *   on the endpoints' perpendicular bisector.
 */
public infix fun Dipole.contract(point: RoundPoint): RoundPoint = RoundPoint(
    ax = -(point.aw * this.gy + point.ay * this.gz + point.az * this.px),   // e1
    ay = point.aw * this.gx + point.ax * this.gz - point.az * this.py,      // e2
    az = point.ay * this.gx - point.ax * this.gy - point.az * this.pz,      // e3
    aw = point.aw * this.pz - point.ax * this.px - point.ay * this.py       // e4
)

/*
CIRCLE
 */

/**
 * `Circle contract RoundPoint => Dipole`<br>
 * Returns the dipole where the circle crosses the radical axis between it and the point.
 *
 * The bulk contraction `c ∨ point^★` — the dipole contained in the circle and orthogonal to
 * the point. Its carrier is the radical axis of the circle and the point taken as a zero
 * circle — the perpendicular bisector of the point and its inversion. A point on the circle
 * contracts to a zero-radius dipole at itself; the dipole is imaginary where the radical
 * axis clears the circle. A point at the circle's center sends the radical axis to infinity:
 * an at-infinity dipole, kept raw — gate with [Dipole.isAtInfinity].
 *
 * @receiver The circle carrying the result.
 * @param point The point contracted away.
 * @return The raw [Dipole] where the circle crosses the radical axis; imaginary when they
 *   miss.
 * @throws DegenerateDipoleException When the whole blade collapses at the `SCALE` quantum —
 *   the point at the center of a circle whose radius also rounds to zero.
 */
public infix fun Circle.contract(point: RoundPoint): Dipole = Dipole(
    gx = -(point.ax * this.cw + point.az * this.cx),        // e23
    gy = -(point.ay * this.cw + point.az * this.cy),        // e31
    gz = point.aw * this.cw - point.az * this.cz,           // e12

    px = point.aw * this.cy + point.ay * this.cz,           // e41
    py = -(point.aw * this.cx + point.ax * this.cz),        // e42
    pz = point.ax * this.cy - point.ay * this.cx            // e43
)

/**
 * `Circle contract FlatPoint => RoundPoint`<br>
 * Returns the circle's tangent direction at the point's bearing, as a point at infinity.
 *
 * The bulk contraction `c ∨ flatPoint^★` — a purely directional object contained in the
 * circle and orthogonal to the flat point: the direction perpendicular to the radial through
 * the point, so a point on the circle contracts to the tangent direction there. The result
 * always lies at infinity and is kept raw — no unitization applies. A point at the circle's
 * center contracts to the zero direction.
 *
 * @receiver The circle contributing its tangent.
 * @param flatPoint The point contracted away.
 * @return The raw at-infinity [RoundPoint] along the tangent bearing; all-zero at the
 *   circle's center.
 */
public infix fun Circle.contract(flatPoint: FlatPoint): RoundPoint = RoundPoint(
    ax = -(this.cw * flatPoint.py + this.cy * flatPoint.pz),    // e1
    ay = this.cw * flatPoint.px + this.cx * flatPoint.pz,       // e2
    az = 0.0,                                                   // e3
    aw = this.cx * flatPoint.py - this.cy * flatPoint.px        // e4
)

/**
 * `Circle contract Dipole => RoundPoint`<br>
 * Returns the round point pairing the circle against the dipole, on the endpoints'
 * perpendicular bisector.
 *
 * The bulk contraction `c ∨ dipole^★` — the round point contained in the circle and
 * orthogonal to the dipole. The result is a genuine round point, not a null lift: its
 * position sits on the endpoints' perpendicular bisector, its raw pairing with the circle
 * (`cx·ax + cy·ay + cz·az + cw·aw`) vanishes, and its container crosses the circle through
 * the endpoints at right angles. A circle centered on the dipole's carrier contracts to a
 * point at infinity, kept raw.
 *
 * @receiver The circle carrying the result.
 * @param dipole The dipole contracted away.
 * @return The raw [RoundPoint] on the endpoints' perpendicular bisector; at infinity when
 *   the circle's center lies on the dipole's carrier.
 */
public infix fun Circle.contract(dipole: Dipole): RoundPoint = RoundPoint(
    ax = -(this.cw * dipole.py + this.cy * dipole.pz + this.cz * dipole.gx),    // e1
    ay = this.cw * dipole.px + this.cx * dipole.pz - this.cz * dipole.gy,       // e2
    az = this.cx * dipole.gx + this.cy * dipole.gy - this.cw * dipole.gz,       // e3
    aw = this.cx * dipole.py - this.cy * dipole.px + this.cz * dipole.gz        // e4
)
