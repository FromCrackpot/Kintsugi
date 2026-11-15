package fromcrackpot.kintsugi.cga.d2.operations.projections

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException


/*
LINE
 */

/**
 * `Line antiproject FlatPoint => Line`<br>
 * Returns the parallel to the line through the point, with this line's normal.
 *
 * @receiver The line whose direction is carried over.
 * @param flatPoint The point the parallel passes through.
 * @return The raw parallel, all-zero for a line at infinity.
 * @throws AtInfinityException when the point lies at infinity.
 */
public infix fun Line.antiproject(flatPoint: FlatPoint): Line {
    if (flatPoint.isAtInfinity) throw AtInfinityException("no parallel through a point at infinity: $flatPoint")
    return Line(
        gx = this.gx,                                                            // e423
        gy = this.gy,                                                            // e431
        gz = -(this.gx * flatPoint.px + this.gy * flatPoint.py) / flatPoint.pz   // e412
    )
}

/**
 * `Line antiproject RoundPoint => Line`<br>
 * Returns the parallel to the line through the point, with this line's normal.
 *
 * - Reads only the point's position.
 *
 * @receiver The line whose direction is carried over.
 * @param point The point the parallel passes through.
 * @return The raw parallel, all-zero for a line at infinity.
 * @throws AtInfinityException when the point lies at infinity.
 */
public infix fun Line.antiproject(point: RoundPoint): Line = this antiproject point.toFlat

/**
 * `Line antiproject Dipole => Circle`<br>
 * Returns the circle through the dipole's endpoints and through the point where the line crosses
 * their perpendicular bisector.
 *
 * - An imaginary dipole negates every component. Coincident endpoints flood the division.
 *
 * @receiver The line bent through the dipole.
 * @param dipole The dipole the circle passes through.
 * @return The raw circle.
 * @throws DegenerateCircleException when the line parallels the dipole's carrier at the `SCALE`
 *   quantum. It carries the carrier.
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
 * Returns the line through the point, perpendicular to its radial. Its normal points toward the
 * circle's center.
 *
 * - The result is a [Line], not a circle. A point target can't carry a round weight.
 *
 * @receiver The circle whose bearing is carried over.
 * @param flatPoint The point the line passes through.
 * @return The raw line. It is the inward tangent for a point on the circle, and all-zero at the center.
 * @throws AtInfinityException when the point lies at infinity.
 */
public infix fun Circle.antiproject(flatPoint: FlatPoint): Line {
    if (flatPoint.isAtInfinity) throw AtInfinityException("no line through a point at infinity: $flatPoint")
    return Line(
        gx = (this.cw * flatPoint.px + this.cx * flatPoint.pz) / flatPoint.pz,   // e423
        gy = (this.cw * flatPoint.py + this.cy * flatPoint.pz) / flatPoint.pz,   // e431
        gz = -(this.cw * (flatPoint.px * flatPoint.px + flatPoint.py * flatPoint.py)
                + (this.cx * flatPoint.px + this.cy * flatPoint.py) * flatPoint.pz)
                / (flatPoint.pz * flatPoint.pz)                                  // e412
    )
}

/**
 * `Circle antiproject RoundPoint => Line`<br>
 * Returns the line through the point, perpendicular to its radial. Its normal points toward the
 * circle's center.
 *
 * - Reads only the point's position.
 *
 * @receiver The circle whose bearing is carried over.
 * @param point The point the line passes through.
 * @return The raw line.
 * @throws AtInfinityException when the point lies at infinity.
 */
public infix fun Circle.antiproject(point: RoundPoint): Line = this antiproject point.toFlat

/**
 * `Circle antiproject Dipole => Circle`<br>
 * Returns the circle through the dipole's endpoints and through this circle's pairing point on
 * their perpendicular bisector.
 *
 * - An imaginary dipole negates every component. Coincident endpoints flood the division.
 *
 * @receiver The circle bent through the dipole.
 * @param dipole The dipole the circle passes through.
 * @return The raw circle.
 * @throws DegenerateCircleException when the round weight collapses at the `SCALE` quantum.
 *   It carries the straightened line.
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
