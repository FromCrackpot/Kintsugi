package fromcrackpot.kintsugi.cga.d2.operations.products

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*

/*
FLAT POINT
 */

/**
 * `FlatPoint expand Line => Line`<br>
 * Returns the line through the point perpendicular to the given line.
 *
 * - A line at infinity gives the all-zero line, not an error.
 *
 * @receiver The point the perpendicular passes through.
 * @param line The line crossed at right angles.
 * @return The raw perpendicular line.
 */
public infix fun FlatPoint.expand(line: Line): Line = Line(
    gx = this.pz * line.gy,                                                  // e423
    gy = -this.pz * line.gx,                                                 // e431
    gz = this.py * line.gx - this.px * line.gy                               // e412
)

/**
 * `FlatPoint expand Circle => Line`<br>
 * Returns the line through the point and the circle's center.
 *
 * - Flipping the circle inside-out reverses the result's orientation.
 * - A point at the center gives the all-zero line, not an error.
 *
 * @receiver The point the radial line passes through.
 * @param circle The circle whose center the line crosses.
 * @return The raw line through the point and the center.
 */
public infix fun FlatPoint.expand(circle: Circle): Line = Line(
    gx = circle.cw * this.py + circle.cy * this.pz,                          // e423
    gy = -(circle.cw * this.px + circle.cx * this.pz),                       // e431
    gz = circle.cx * this.py - circle.cy * this.px                           // e412
)


/*
ROUND POINT
 */

/**
 * `RoundPoint expand Line => Dipole`<br>
 * Returns the dipole holding the point and its mirror image across the line.
 *
 * @receiver The point to pair with its reflection.
 * @param line The mirror line.
 * @return The raw dipole, zero-radius for a point on the line.
 * @throws DegenerateDipoleException when the line lies at infinity, or a point at infinity runs
 *   along the line's normal.
 */
public infix fun RoundPoint.expand(line: Line): Dipole = Dipole(
    gx = this.az * line.gy,                                 // e23
    gy = -this.az * line.gx,                                // e31
    gz = this.ay * line.gx - this.ax * line.gy,             // e12

    px = -(this.ax * line.gz + this.aw * line.gx),          // e41
    py = -(this.ay * line.gz + this.aw * line.gy),          // e42
    pz = -this.az * line.gz                                 // e43
)

/**
 * `RoundPoint expand Circle => Dipole`<br>
 * Returns the dipole holding the point and its inversion through the circle.
 *
 * - Flipping the circle inside-out reverses the result's orientation.
 *
 * @receiver The point to pair with its inversion.
 * @param circle The inverting circle.
 * @return The raw dipole, zero-radius for a point on the circle.
 * @throws DegenerateDipoleException when the point sits at the circle's center at the `SCALE` quantum.
 */
public infix fun RoundPoint.expand(circle: Circle): Dipole = Dipole(
    gx = this.ay * circle.cw + this.az * circle.cy,         // e23
    gy = -(this.ax * circle.cw + this.az * circle.cx),      // e31
    gz = this.ay * circle.cx - this.ax * circle.cy,         // e12

    px = -(this.ax * circle.cz + this.aw * circle.cx),      // e41
    py = -(this.ay * circle.cz + this.aw * circle.cy),      // e42
    pz = this.aw * circle.cw - this.az * circle.cz          // e43
)

/**
 * `RoundPoint expand Dipole => Circle`<br>
 * Returns the circle through the point whose inversion swaps the dipole's endpoints.
 *
 * - Swapping the dipole's endpoints reverses the result's orientation.
 *
 * @receiver The point the circle passes through.
 * @param dipole The dipole whose endpoints the circle swaps.
 * @return The raw circle.
 * @throws DegenerateCircleException when a finite point lies on the endpoints' perpendicular
 *   bisector. It carries that line.
 */
public infix fun RoundPoint.expand(dipole: Dipole): Circle = Circle(
    cx = -(this.az * dipole.px + this.ay * dipole.gz + this.aw * dipole.gy), // e423
    cy = this.ax * dipole.gz + this.aw * dipole.gx - this.az * dipole.py,    // e431
    cz = this.ax * dipole.px + this.ay * dipole.py - this.aw * dipole.pz,    // e412
    cw = this.ax * dipole.gy - this.ay * dipole.gx + this.az * dipole.pz     // e321
)

/**
 * `RoundPoint expand FlatPoint => Circle`<br>
 * Returns the circle through the round point centered at the flat point.
 *
 * - The circle comes out inside-out.
 * - A sized round point adds its own `radius2` to the squared distance.
 *
 * @receiver The point the circle passes through.
 * @param flatPoint The circle's center.
 * @return The raw circle.
 * @throws DegenerateCircleException when either operand lies at infinity at the `SCALE` quantum.
 */
public infix fun RoundPoint.expand(flatPoint: FlatPoint): Circle = Circle(
    cx = -this.az * flatPoint.px,                                            // e423
    cy = -this.az * flatPoint.py,                                            // e431
    cz = this.ax * flatPoint.px + this.ay * flatPoint.py - this.aw * flatPoint.pz, // e412
    cw = this.az * flatPoint.pz                                              // e321
)

/*
DIPOLE
 */

/**
 * `Dipole expand Line => Circle`<br>
 * Returns the circle through the dipole's endpoints that crosses the line at right angles.
 *
 * - Swapping the dipole's endpoints reverses the result's orientation.
 *
 * @receiver The dipole contributing its endpoints.
 * @param line The line crossed at right angles.
 * @return The raw circle.
 * @throws DegenerateCircleException when the dipole lies at infinity, or when its carrier is
 *   perpendicular to the line. The latter carries the carrier.
 */
public infix fun Dipole.expand(line: Line): Circle = Circle(
    cx = this.gx * line.gz + this.pz * line.gy,                              // e423
    cy = this.gy * line.gz - this.pz * line.gx,                              // e431
    cz = this.gz * line.gz - this.px * line.gy + this.py * line.gx,          // e412
    cw = this.gx * line.gx + this.gy * line.gy                               // e321
)

/**
 * `Dipole expand Circle => Circle`<br>
 * Returns the circle through the dipole's endpoints that crosses the circle at right angles.
 *
 * - Swapping the endpoints or flipping the circle inside-out reverses the result's orientation.
 *
 * @receiver The dipole contributing its endpoints.
 * @param circle The circle crossed at right angles.
 * @return The raw circle.
 * @throws DegenerateCircleException when the circle's center lies on the dipole's carrier.
 *   It carries the carrier.
 */
public infix fun Dipole.expand(circle: Circle): Circle = Circle(
    cx = circle.cz * this.gx + circle.cy * this.pz + circle.cw * this.py,    // e423
    cy = circle.cz * this.gy - circle.cx * this.pz - circle.cw * this.px,    // e431
    cz = circle.cx * this.py - circle.cy * this.px + circle.cz * this.gz,    // e412
    cw = circle.cx * this.gx + circle.cy * this.gy - circle.cw * this.gz     // e321
)
