package fromcrackpot.kintsugi.cga.d2.products

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
 * The weight expansion `p ∧ line^☆` — the line containing this point and orthogonal to
 * [line]. Its normal runs along [line]'s own direction, so the result's orientation follows
 * the operand orientations. A point at infinity expands into a line at infinity, kept raw;
 * a line at infinity collapses to the all-zero line, which the [Line] constructor accepts.
 *
 * @receiver The point the perpendicular passes through.
 * @param line The line crossed at right angles.
 * @return The raw [Line] through the point, perpendicular to [line]; all-zero when [line]
 *   lies at infinity.
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
 * The weight expansion `p ∧ circle^☆` — the line containing this point and orthogonal to the
 * circle: a line crosses a circle at right angles exactly when it passes through the center.
 * Flipping the circle inside-out negates every component: the same line, opposite
 * orientation. A point at the circle's center collapses to the all-zero line, which the
 * [Line] constructor accepts.
 *
 * @receiver The point the radial line passes through.
 * @param circle The circle whose center the line crosses.
 * @return The raw [Line] through the point and the circle's center; all-zero when they
 *   coincide.
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
 * The weight expansion `a ∧ line^☆` — the dipole containing this point and orthogonal to the
 * line. Its carrier is the perpendicular dropped from the point onto the line, and its
 * endpoints are the point and its reflection, so a point on the line yields a zero-radius
 * dipole at the point itself. A point at infinity off the line's normal expands into an
 * at-infinity dipole, kept raw — gate with [Dipole.isAtInfinity].
 *
 * @receiver The point to pair with its reflection.
 * @param line The mirror line.
 * @return The raw [Dipole] holding the point and its reflection.
 * @throws DegenerateDipoleException When the blade collapses at the `SCALE` quantum — the
 *   line at infinity, or a point at infinity running along the line's normal.
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
 * The weight expansion `a ∧ circle^☆` — the dipole containing this point and orthogonal to
 * the circle. Its carrier is the radial line through the point, and its endpoints are the
 * point and its circle inversion, so a point on the circle yields a zero-radius dipole at the
 * point itself. Flipping the circle inside-out negates every component: the same pair,
 * opposite orientation.
 *
 * @receiver The point to pair with its inversion.
 * @param circle The inverting circle.
 * @return The raw [Dipole] holding the point and its inversion.
 * @throws DegenerateDipoleException When the point sits at the circle's center at the `SCALE`
 *   quantum — its inversion escapes to infinity and the blade collapses.
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
 * The weight expansion `a ∧ dipole^☆` — the circle containing this point and orthogonal to
 * the dipole. Inverting through the result maps the dipole's endpoints onto each other;
 * equivalently, the result cuts the carrier in a pair separating the endpoints harmonically.
 * Swapping the dipole's endpoints negates every component: the same circle, opposite
 * orientation.
 *
 * @receiver The point the circle passes through.
 * @param dipole The dipole whose endpoints the circle swaps.
 * @return The raw [Circle] through the point, orthogonal to the dipole.
 * @throws DegenerateCircleException When the round weight collapses at the `SCALE` quantum —
 *   for a finite point, exactly when it lies on the endpoints' perpendicular bisector: the
 *   swap-circle straightens into that mirror line, carried as the payload.
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
 * The weight expansion `a ∧ flatPoint^☆` — the circle containing this point and orthogonal
 * to the flat point. The expansion builds the circle inside-out (`cw = +1` for unitized
 * finite operands — what the algebra gives); its `radius2` is the squared distance between
 * the operands, plus this point's own `radius2` when it carries one.
 *
 * @receiver The point the circle passes through.
 * @param flatPoint The circle's center.
 * @return The raw [Circle] through the round point, centered at the flat point.
 * @throws DegenerateCircleException When either operand lies at infinity at the `SCALE`
 *   quantum — the round weight collapses; a center at infinity leaves the line through the
 *   round point perpendicular to the carried direction as the payload.
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
 * The weight expansion `d ∧ line^☆` — the circle containing this dipole and orthogonal to
 * the line: its center sits on the line, so the line cuts it along a diameter. Swapping the
 * dipole's endpoints negates every component: the same circle, opposite orientation.
 *
 * @receiver The dipole contributing its endpoints.
 * @param line The line crossed at right angles.
 * @return The raw [Circle] through the dipole's endpoints, orthogonal to the line.
 * @throws DegenerateCircleException When the round weight collapses at the `SCALE` quantum —
 *   the dipole's carrier perpendicular to the line, where the sought circle straightens into
 *   the carrier itself (the payload), or the dipole at infinity.
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
 * The weight expansion `d ∧ circle^☆` — the circle containing this dipole and orthogonal to
 * the circle: inverting through [circle] maps the result onto itself. Swapping the dipole's
 * endpoints, or flipping the circle inside-out, negates every component: the same circle,
 * opposite orientation.
 *
 * @receiver The dipole contributing its endpoints.
 * @param circle The circle crossed at right angles.
 * @return The raw [Circle] through the dipole's endpoints, orthogonal to the circle.
 * @throws DegenerateCircleException When the round weight collapses at the `SCALE` quantum —
 *   exactly when the circle's center lies on the dipole's carrier: the sought circle
 *   straightens into the carrier itself, carried as the payload.
 */
public infix fun Dipole.expand(circle: Circle): Circle = Circle(
    cx = circle.cz * this.gx + circle.cy * this.pz + circle.cw * this.py,    // e423
    cy = circle.cz * this.gy - circle.cx * this.pz - circle.cw * this.px,    // e431
    cz = circle.cx * this.py - circle.cy * this.px + circle.cz * this.gz,    // e412
    cw = circle.cx * this.gx + circle.cy * this.gy - circle.cw * this.gz     // e321
)
