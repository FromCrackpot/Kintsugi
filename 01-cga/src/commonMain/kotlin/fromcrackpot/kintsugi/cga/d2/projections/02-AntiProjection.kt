package fromcrackpot.kintsugi.cga.d2.projections

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException


/*
LINE
 */

/**
 * `Line antiproject FlatPoint => Line`<br>
 * Returns the parallel to the line through the point.
 *
 * The antiprojection `flatPoint ∧ (g ∨ flatPoint^☆)`, normalized by the flat point's
 * self-pairing `flatPoint ∘ flatPoint` so the parallel keeps this line's weight and
 * orientation — its normal points the same way. A point on the line antiprojects the line
 * onto itself, and a line at infinity antiprojects to the all-zero line, which the [Line]
 * constructor accepts. A point at infinity is not rejected: its self-pairing vanishes and
 * the division floods the components.
 *
 * @receiver The line whose direction is carried over.
 * @param flatPoint The point the parallel passes through.
 * @return The raw [Line] through the point, parallel to this line.
 */
public infix fun Line.antiproject(flatPoint: FlatPoint): Line = Line(
    gx = this.gx,                                                            // e423
    gy = this.gy,                                                            // e431
    gz = -(this.gx * flatPoint.px + this.gy * flatPoint.py) / flatPoint.pz   // e412
)

/**
 * `Line antiproject RoundPoint => Line`<br>
 * Returns the parallel to the line through the point.
 *
 * `Line antiproject FlatPoint` through [RoundPoint.toFlat] — the antiprojection reads only
 * the target's location, so the round form delegates to the flat one; a conformal component
 * carrying a size plays no part. A point at infinity is not rejected: the delegated
 * division floods the components.
 *
 * @receiver The line whose direction is carried over.
 * @param point The point the parallel passes through.
 * @return The raw [Line] through the point, parallel to this line.
 */
public infix fun Line.antiproject(point: RoundPoint): Line = this antiproject point.toFlat

/**
 * `Line antiproject Dipole => Circle`<br>
 * Returns the circle through the dipole's endpoints incident with the line's crossing of
 * their perpendicular bisector.
 *
 * The antiprojection `dipole ∧ (g ∨ dipole^☆)`, normalized by the dipole's self-pairing
 * `dipole ∘ dipole` — its raw signed [Dipole.radius2], so an imaginary dipole negates every
 * component. The circle passes through the dipole's endpoints, and its raw pairing with the
 * sized crossing point `g contract dipole` vanishes. A line perpendicular to the carrier
 * bends into the circle with the endpoints as a diameter. A dipole with coincident endpoints
 * is not rejected: its self-pairing vanishes and the division floods the components.
 *
 * @receiver The line bent through the dipole.
 * @param dipole The dipole the circle passes through.
 * @return The raw [Circle] through the dipole's endpoints.
 * @throws DegenerateCircleException When the line parallels the dipole's carrier at the
 *   `SCALE` quantum — the bent circle straightens into the carrier, carried as the payload.
 */
public infix fun Line.antiproject(dipole: Dipole): Circle {
    val n = dipole.radius2
    return Circle(
        cx = (dipole.pz * dipole.pz * this.gx - dipole.gx * dipole.gz * this.gz
                + dipole.gx * dipole.px * this.gy - 2 * dipole.gx * dipole.py * this.gx
                - dipole.gy * dipole.py * this.gy - dipole.gy * dipole.pz * this.gz) / n, // e423
        cy = (dipole.pz * dipole.pz * this.gy + dipole.gx * dipole.px * this.gx
                + dipole.gx * dipole.pz * this.gz - dipole.gy * dipole.gz * this.gz
                + 2 * dipole.gy * dipole.px * this.gy - dipole.gy * dipole.py * this.gx) / n, // e431
        cz = (dipole.gy * dipole.px * this.gz - dipole.gx * dipole.py * this.gz
                - dipole.gz * dipole.gz * this.gz + dipole.gz * dipole.px * this.gy
                - dipole.gz * dipole.py * this.gx - dipole.px * dipole.pz * this.gx
                - dipole.py * dipole.pz * this.gy) / n,                                   // e412
        cw = (dipole.gx * dipole.gz * this.gx + dipole.gy * dipole.gz * this.gy
                - (dipole.gx * dipole.gx + dipole.gy * dipole.gy) * this.gz
                - dipole.gx * dipole.pz * this.gy + dipole.gy * dipole.pz * this.gx) / n  // e321
    )
}

/*
CIRCLE
 */

/**
 * `Circle antiproject FlatPoint => Line`<br>
 * Returns the line through the point running the circle's way at the point's bearing —
 * perpendicular to the radial through it.
 *
 * The antiprojection `flatPoint ∧ (c ∨ flatPoint^☆)`, normalized by the flat point's
 * self-pairing `flatPoint ∘ flatPoint`. The result is a line, not a circle: every flat-point
 * blade carries a factor of `e4`, so the outer wedge can never rebuild a round weight — the
 * flat limit of carrying the circle's attitude over to the point. Its normal points from the
 * point toward the circle's center, so a point on the circle antiprojects to the tangent
 * there, oriented inward. A point at the circle's center antiprojects to the all-zero line,
 * which the [Line] constructor accepts; a point at infinity is not rejected: its
 * self-pairing vanishes and the division floods the components.
 *
 * @receiver The circle whose bearing is carried over.
 * @param flatPoint The point the line passes through.
 * @return The raw [Line] through the point, perpendicular to its radial; all-zero at the
 *   circle's center.
 */
public infix fun Circle.antiproject(flatPoint: FlatPoint): Line = Line(
    gx = (this.cw * flatPoint.px + this.cx * flatPoint.pz) / flatPoint.pz,   // e423
    gy = (this.cw * flatPoint.py + this.cy * flatPoint.pz) / flatPoint.pz,   // e431
    gz = -(this.cw * (flatPoint.px * flatPoint.px + flatPoint.py * flatPoint.py)
            + (this.cx * flatPoint.px + this.cy * flatPoint.py) * flatPoint.pz)
            / (flatPoint.pz * flatPoint.pz)                                  // e412
)

/**
 * `Circle antiproject RoundPoint => Line`<br>
 * Returns the line through the point running the circle's way at the point's bearing —
 * perpendicular to the radial through it.
 *
 * `Circle antiproject FlatPoint` through [RoundPoint.toFlat] — the antiprojection reads only
 * the target's location, so the round form delegates to the flat one; a conformal component
 * carrying a size plays no part. A point at infinity is not rejected: the delegated
 * division floods the components.
 *
 * @receiver The circle whose bearing is carried over.
 * @param point The point the line passes through.
 * @return The raw [Line] through the point, perpendicular to its radial.
 */
public infix fun Circle.antiproject(point: RoundPoint): Line = this antiproject point.toFlat

/**
 * `Circle antiproject Dipole => Circle`<br>
 * Returns the circle through the dipole's endpoints incident with this circle's pairing
 * point on their perpendicular bisector.
 *
 * The antiprojection `dipole ∧ (c ∨ dipole^☆)`, normalized by the dipole's self-pairing
 * `dipole ∘ dipole` — its raw signed [Dipole.radius2], so an imaginary dipole negates every
 * component. The result passes through the dipole's endpoints and its raw pairing with the
 * sized point `c contract dipole` vanishes; a circle already through the endpoints
 * antiprojects to itself. A dipole with coincident endpoints is not rejected: its
 * self-pairing vanishes and the division floods the components.
 *
 * @receiver The circle bent through the dipole.
 * @param dipole The dipole the circle passes through.
 * @return The raw [Circle] through the dipole's endpoints.
 * @throws DegenerateCircleException When the round weight collapses at the `SCALE` quantum —
 *   the bent circle straightens into a line, carried as the payload.
 */
public infix fun Circle.antiproject(dipole: Dipole): Circle {
    val n = dipole.radius2
    return Circle(
        cx = (this.cw * dipole.gz * dipole.py + this.cw * dipole.px * dipole.pz
                - 2 * this.cx * dipole.gx * dipole.py + this.cx * dipole.pz * dipole.pz
                + this.cy * dipole.gx * dipole.px - this.cy * dipole.gy * dipole.py
                - this.cz * dipole.gx * dipole.gz - this.cz * dipole.gy * dipole.pz) / n, // e423
        cy = (this.cw * dipole.py * dipole.pz - this.cw * dipole.gz * dipole.px
                + this.cx * dipole.gx * dipole.px - this.cx * dipole.gy * dipole.py
                + 2 * this.cy * dipole.gy * dipole.px + this.cy * dipole.pz * dipole.pz
                + this.cz * dipole.gx * dipole.pz - this.cz * dipole.gy * dipole.gz) / n, // e431
        cz = (this.cy * dipole.gz * dipole.px - this.cw * dipole.px * dipole.px
                - this.cw * dipole.py * dipole.py - this.cx * dipole.gz * dipole.py
                - this.cx * dipole.px * dipole.pz - this.cy * dipole.py * dipole.pz
                - this.cz * dipole.gx * dipole.py + this.cz * dipole.gy * dipole.px
                - this.cz * dipole.gz * dipole.gz) / n,                                   // e412
        cw = (this.cw * dipole.gy * dipole.px - this.cw * dipole.gx * dipole.py
                - this.cw * dipole.gz * dipole.gz + this.cx * dipole.gx * dipole.gz
                + this.cx * dipole.gy * dipole.pz - this.cy * dipole.gx * dipole.pz
                + this.cy * dipole.gy * dipole.gz - this.cz * dipole.gx * dipole.gx
                - this.cz * dipole.gy * dipole.gy) / n                                    // e321
    )
}
