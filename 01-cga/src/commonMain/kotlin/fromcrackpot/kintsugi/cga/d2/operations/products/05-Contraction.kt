package fromcrackpot.kintsugi.cga.d2.operations.products

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
 * @receiver The flat point contributing its position.
 * @param point The point contracted away.
 * @return The raw round point at the flat point's position, imaginary when the operands sit apart.
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
 * Returns the dipole on the line, centered at the point's perpendicular foot. Its squared radius
 * is minus the squared distance to the line.
 *
 * - The result's carrier runs opposite this line's orientation.
 *
 * @receiver The line carrying the result.
 * @param point The point contracted away.
 * @return The raw dipole, zero-radius for a point on the line and imaginary off it.
 * @throws DegenerateDipoleException when the point lies at infinity at the `SCALE` quantum.
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
 * @receiver The line contributing its direction.
 * @param flatPoint The point contracted away.
 * @return The raw at-infinity round point along the line's direction.
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
 * - The result is a sized round point, not a plain lifted one.
 *
 * @receiver The line carrying the result.
 * @param dipole The dipole contracted away.
 * @return The raw round point at the crossing, at infinity when the line parallels the bisector.
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
 * Returns the center of the circle that swaps the dipole's endpoints through the point. It carries
 * that circle's squared radius, negated.
 *
 * @receiver The dipole carrying the result.
 * @param point The point contracted away.
 * @return The raw round point at the swap-circle's center, at infinity when the point lies on the
 *   endpoints' perpendicular bisector.
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
 * - A point at the circle's center gives an at-infinity dipole, kept raw.
 *
 * @receiver The circle carrying the result.
 * @param point The point contracted away.
 * @return The raw dipole, zero-radius for a point on the circle and imaginary when the axis misses it.
 * @throws DegenerateDipoleException when the point sits at the center of a circle whose radius
 *   also rounds to zero.
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
 * @receiver The circle contributing its tangent.
 * @param flatPoint The point contracted away.
 * @return The raw at-infinity round point along the tangent bearing, all-zero at the circle's center.
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
 * @receiver The circle carrying the result.
 * @param dipole The dipole contracted away.
 * @return The raw round point, at infinity when the circle's center lies on the dipole's carrier.
 */
public infix fun Circle.contract(dipole: Dipole): RoundPoint = RoundPoint(
    ax = -(this.cw * dipole.py + this.cy * dipole.pz + this.cz * dipole.gx),    // e1
    ay = this.cw * dipole.px + this.cx * dipole.pz - this.cz * dipole.gy,       // e2
    az = this.cx * dipole.gx + this.cy * dipole.gy - this.cw * dipole.gz,       // e3
    aw = this.cx * dipole.py - this.cy * dipole.px + this.cz * dipole.gz        // e4
)
