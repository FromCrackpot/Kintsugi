package fromcrackpot.kintsugi.cga.d2.operations.projections

import fromcrackpot.kintsugi.cga.d2.exceptions.AtInfinityException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonBladeDipoleException
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
// Upward reach by design. The `d2/` tiers are a reading order, not a dependency DAG.
import fromcrackpot.kintsugi.cga.d2.operations.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.operations.queries.predicates.on

/*
FLAT POINT
 */

/**
 * `FlatPoint project Line => FlatPoint`<br>
 * Returns the point's perpendicular foot on the line.
 *
 * - A direction (at infinity) projects to its component along the line.
 *
 * @receiver The point dropped onto the line.
 * @param line The line projected onto.
 * @return The raw foot.
 * @throws AtInfinityException when the line lies at infinity.
 */
public infix fun FlatPoint.project(line: Line): FlatPoint {
    if (line.isAtInfinity) throw AtInfinityException("no projection onto a line at infinity: $line")
    val t = (this dot line) / (line.gx * line.gx + line.gy * line.gy)
    return FlatPoint(
        px = this.px - line.gx * t, // e41
        py = this.py - line.gy * t, // e42
        pz = this.pz                // e43
    )
}

/**
 * `FlatPoint project Circle => Dipole`<br>
 * Returns the dipole where the circle crosses the radial line through the point.
 *
 * - An imaginary circle negates every component.
 *
 * @receiver The point whose bearing cuts the circle.
 * @param circle The circle projected onto.
 * @return The raw dipole.
 * @throws DegenerateDipoleException when the point sits at the circle's center at the `SCALE` quantum.
 * @throws NonBladeDipoleException when the circle's radius is exactly zero.
 */
public infix fun FlatPoint.project(circle: Circle): Dipole {
    val n = circle.radius2
    return Dipole(
        gx = -(circle.cw * circle.cw * this.py + circle.cw * circle.cy * this.pz) / n,   // e23
        gy = (circle.cw * circle.cw * this.px + circle.cw * circle.cx * this.pz) / n,    // e31
        gz = (circle.cw * (circle.cy * this.px - circle.cx * this.py)) / n,              // e12

        px = (circle.cy * circle.cy * this.px - circle.cw * circle.cz * this.px
                - circle.cx * circle.cy * this.py - circle.cx * circle.cz * this.pz) / n, // e41
        py = (circle.cx * circle.cx * this.py - circle.cw * circle.cz * this.py
                - circle.cx * circle.cy * this.px - circle.cy * circle.cz * this.pz) / n, // e42
        pz = (circle.cw * (circle.cx * this.px + circle.cy * this.py)
                + (circle.cx * circle.cx + circle.cy * circle.cy) * this.pz) / n          // e43
    )
}

/**
 * `FlatPoint project Segment => FlatPoint`<br>
 * Returns the point of the segment nearest to this point: the perpendicular foot when it falls
 * within the segment, else the nearer endpoint.
 *
 * @receiver The point dropped onto the segment.
 * @param segment The segment projected onto.
 * @return The raw nearest point.
 * @throws AtInfinityException when the point or the segment lies at infinity.
 */
public infix fun FlatPoint.project(segment: Segment): FlatPoint {
    if (isAtInfinity || segment.toDipole.isAtInfinity) {
        throw AtInfinityException("no nearest point with an operand at infinity: $this, $segment")
    }
    val foot = this project segment.carrier
    if (foot on segment) return foot
    val start = segment.start.toFlat
    val end = segment.end.toFlat
    return if (this distanceTo start <= this distanceTo end) start else end
}

/*
ROUND POINT
 */

/**
 * `RoundPoint project Line => RoundPoint`<br>
 * Returns the point's perpendicular foot on the line, sized by the squared distance to it.
 *
 * @receiver The point dropped onto the line.
 * @param line The line projected onto.
 * @return The raw sized foot.
 * @throws AtInfinityException when the line lies at infinity.
 */
public infix fun RoundPoint.project(line: Line): RoundPoint {
    if (line.isAtInfinity) throw AtInfinityException("no projection onto a line at infinity: $line")
    val t = (this dot line) / (line.gx * line.gx + line.gy * line.gy)
    return RoundPoint(
        ax = this.ax - line.gx * t, // e1
        ay = this.ay - line.gy * t, // e2
        az = this.az,               // e3
        aw = this.aw + line.gz * t  // e4
    )
}

/**
 * `RoundPoint project Circle => RoundPoint`<br>
 * Returns the round point that pairs the point into the circle. It lies on the circle, on the
 * radial line through the point.
 *
 * - An imaginary circle negates every component. A zero-radius one floods the division.
 *
 * @receiver The point dropped onto the circle.
 * @param circle The circle projected onto.
 * @return The raw round point encoding the radial crossing pair.
 */
public infix fun RoundPoint.project(circle: Circle): RoundPoint {
    val t = (this dot circle) / circle.radius2
    return RoundPoint(
        ax = this.ax - circle.cx * t, // e1
        ay = this.ay - circle.cy * t, // e2
        az = this.az + circle.cw * t, // e3
        aw = this.aw + circle.cz * t  // e4
    )
}

/**
 * `RoundPoint project Dipole => RoundPoint`<br>
 * Returns the point of the dipole's carrier dividing the endpoints in the ratio of their
 * squared distances from the given point.
 *
 * - An imaginary dipole negates every component. Coincident endpoints flood the division.
 *
 * @receiver The point dropped onto the dipole.
 * @param dipole The dipole projected onto.
 * @return The raw sized round point on the carrier.
 */
public infix fun RoundPoint.project(dipole: Dipole): RoundPoint {
    val n = dipole.radius2
    return RoundPoint(
        ax = (2 * this.ax * dipole.gy * dipole.px - this.aw * dipole.gx * dipole.gz
                - this.aw * dipole.gy * dipole.pz - this.ax * dipole.gz * dipole.gz
                - this.ay * dipole.gx * dipole.px + this.ay * dipole.gy * dipole.py
                + this.az * dipole.gz * dipole.py + this.az * dipole.px * dipole.pz) / n, // e1
        ay = (this.aw * dipole.gx * dipole.pz - this.aw * dipole.gy * dipole.gz
                - this.ax * dipole.gx * dipole.px + this.ax * dipole.gy * dipole.py
                - 2 * this.ay * dipole.gx * dipole.py - this.ay * dipole.gz * dipole.gz
                - this.az * dipole.gz * dipole.px + this.az * dipole.py * dipole.pz) / n, // e2
        az = (this.aw * (dipole.gx * dipole.gx + dipole.gy * dipole.gy)
                + this.ax * dipole.gx * dipole.gz + this.ax * dipole.gy * dipole.pz
                - this.ay * dipole.gx * dipole.pz + this.ay * dipole.gy * dipole.gz
                - this.az * dipole.gx * dipole.py + this.az * dipole.gy * dipole.px
                + this.az * dipole.pz * dipole.pz) / n,                                   // e3
        aw = (this.aw * dipole.gy * dipole.px - this.aw * dipole.gx * dipole.py
                + this.aw * dipole.pz * dipole.pz - this.ax * dipole.gz * dipole.py
                - this.ax * dipole.px * dipole.pz + this.ay * dipole.gz * dipole.px
                - this.ay * dipole.py * dipole.pz
                + this.az * (dipole.px * dipole.px + dipole.py * dipole.py)) / n          // e4
    )
}

/*
DIPOLE
 */

/**
 * `Dipole project Line => Dipole`<br>
 * Returns the dipole where the line cuts the circle that passes through this dipole's endpoints
 * and is centered on the line.
 *
 * @receiver The dipole dropped onto the line.
 * @param line The line projected onto.
 * @return The raw dipole on the line.
 * @throws AtInfinityException when the line lies at infinity.
 * @throws DegenerateDipoleException when the dipole's carrier is perpendicular to the line at the
 *   `SCALE` quantum.
 */
public infix fun Dipole.project(line: Line): Dipole {
    if (line.isAtInfinity) throw AtInfinityException("no projection onto a line at infinity: $line")
    val n = line.gx * line.gx + line.gy * line.gy
    val k = this.gx * line.gx + this.gy * line.gy
    return Dipole(
        gx = line.gx * k / n,   // e23
        gy = line.gy * k / n,   // e31
        gz = line.gz * k / n,   // e12

        px = (this.gy * line.gz * line.gz - this.gz * line.gy * line.gz
                + this.px * line.gy * line.gy - this.py * line.gx * line.gy
                - this.pz * line.gx * line.gz) / n,                          // e41
        py = (this.gz * line.gx * line.gz - this.gx * line.gz * line.gz
                - this.px * line.gx * line.gy + this.py * line.gx * line.gx
                - this.pz * line.gy * line.gz) / n,                          // e42
        pz = this.pz + (this.gx * line.gy * line.gz - this.gy * line.gx * line.gz) / n // e43
    )
}

/**
 * `Dipole project Circle => Dipole`<br>
 * Returns the dipole where [circle] meets the circle through this dipole's endpoints that is
 * orthogonal to [circle].
 *
 * - An imaginary circle negates every component.
 *
 * @receiver The dipole dropped onto the circle.
 * @param circle The circle projected onto.
 * @return The raw dipole on the circle.
 * @throws DegenerateDipoleException when the whole blade collapses at the `SCALE` quantum.
 * @throws NonBladeDipoleException when the circle's radius is exactly zero.
 */
public infix fun Dipole.project(circle: Circle): Dipole {
    val n = circle.radius2
    return Dipole(
        gx = (circle.cx * circle.cx * this.gx + circle.cx * circle.cy * this.gy
                - circle.cw * circle.cw * this.py - circle.cw * circle.cx * this.gz
                - circle.cw * circle.cy * this.pz - circle.cw * circle.cz * this.gx) / n, // e23
        gy = (circle.cx * circle.cy * this.gx + circle.cy * circle.cy * this.gy
                + circle.cw * circle.cw * this.px + circle.cw * circle.cx * this.pz
                - circle.cw * circle.cy * this.gz - circle.cw * circle.cz * this.gy) / n, // e31
        gz = (circle.cw * circle.cy * this.px - circle.cw * circle.cx * this.py
                - 2 * circle.cw * circle.cz * this.gz + circle.cx * circle.cz * this.gx
                + circle.cy * circle.cz * this.gy) / n,                                   // e12

        px = (circle.cy * circle.cy * this.px - circle.cw * circle.cz * this.px
                - circle.cx * circle.cy * this.py - circle.cx * circle.cz * this.pz
                - circle.cy * circle.cz * this.gz + circle.cz * circle.cz * this.gy) / n, // e41
        py = (circle.cx * circle.cx * this.py - circle.cw * circle.cz * this.py
                - circle.cx * circle.cy * this.px + circle.cx * circle.cz * this.gz
                - circle.cy * circle.cz * this.pz - circle.cz * circle.cz * this.gx) / n, // e42
        pz = (circle.cw * circle.cx * this.px + circle.cw * circle.cy * this.py
                + (circle.cx * circle.cx + circle.cy * circle.cy) * this.pz
                - circle.cx * circle.cz * this.gy + circle.cy * circle.cz * this.gx) / n  // e43
    )
}
