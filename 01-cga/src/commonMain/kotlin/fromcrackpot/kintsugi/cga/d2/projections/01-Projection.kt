package fromcrackpot.kintsugi.cga.d2.projections

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonBladeDipoleException
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
// Upward reach by design — the `d2/` tiers are a reading order, not a dependency DAG.
import fromcrackpot.kintsugi.cga.d2.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on

/*
FLAT POINT
 */

/**
 * `FlatPoint project Line => FlatPoint`<br>
 * Returns the point's perpendicular foot on the line.
 *
 * The projection `line ∨ (p ∧ line^☆)`, normalized by the line's self-pairing
 * `line ∘ line` — a point on the line projects to itself, and the weight carries through
 * untouched. A point at infinity projects to its component along the line: the classic
 * vector projection of a free vector onto the line's direction. A line at infinity is not
 * rejected: its self-pairing vanishes and the division floods the components.
 *
 * @receiver The point dropped onto the line.
 * @param line The line projected onto.
 * @return The raw [FlatPoint] at the perpendicular foot.
 */
public infix fun FlatPoint.project(line: Line): FlatPoint {
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
 * The projection `circle ∨ (p ∧ circle^☆)`, normalized by the circle's self-pairing
 * `circle ∘ circle` — its raw signed `radius2`, so an imaginary circle negates every
 * component. The radial line always passes through the circle's center, so against a classic
 * circle the crossing is real: the endpoints are the near and far circle points on the
 * point's bearing. A circle of exactly zero radius has a zero self-pairing: the division
 * floods the blade and the constructor rejects it.
 *
 * @receiver The point whose bearing cuts the circle.
 * @param circle The circle projected onto.
 * @return The raw [Dipole] where the circle crosses the radial line.
 * @throws DegenerateDipoleException When the point sits at the circle's center at the
 *   `SCALE` quantum — every bearing is radial and the blade collapses.
 * @throws NonBladeDipoleException When the circle's self-pairing is zero — the flooded
 *   blade fails the constructor's check.
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
 * Returns the point of the segment nearest to this point: the perpendicular foot when it falls within
 * the segment, else the nearer endpoint.
 *
 * The bent form of `FlatPoint project Line`: the foot on [Segment.carrier], kept when it lies on the
 * segment itself — the endpoints included — and replaced by the endpoint nearer to this point when it
 * falls beyond either end. The bounds decision rounds at the `SCALE` quantum while the returned point
 * stays raw, so a foot within the quantum past an endpoint comes back as the raw foot. A point already
 * on the segment projects to itself. A point at infinity — zero weight — is a free vector rather than a
 * position, and its answer is no nearest point; gate with [FlatPoint.isAtInfinity]. A segment at
 * infinity is not rejected: its carrier's self-pairing vanishes and the division floods the components.
 *
 * @receiver The point dropped onto the segment.
 * @param segment The segment projected onto.
 * @return The raw [FlatPoint] on the segment nearest to this point.
 */
public infix fun FlatPoint.project(segment: Segment): FlatPoint {
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
 * The projection `line ∨ (a ∧ line^☆)`, normalized by the line's self-pairing `line ∘ line`.
 * The result is a genuine round point, not a null lift: its position is the foot of the
 * perpendicular and its raw `radius2` is `+d²` for a finite null-lift operand `d` away — a
 * point on the line projects to itself. A point at infinity projects to a point at infinity,
 * kept raw. A line at infinity is not rejected: its self-pairing vanishes and the division
 * floods the components.
 *
 * @receiver The point dropped onto the line.
 * @param line The line projected onto.
 * @return The raw [RoundPoint] at the perpendicular foot, sized by the squared distance.
 */
public infix fun RoundPoint.project(line: Line): RoundPoint {
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
 * Returns the round point that pairs the point into the circle: on the radial line through
 * the point, incident with the circle.
 *
 * The projection `circle ∨ (a ∧ circle^☆)`, normalized by the circle's self-pairing
 * `circle ∘ circle` — its raw signed `radius2`, so an imaginary circle negates every
 * component. The result is a genuine round point, not a null lift: it sits on the radial
 * line through the point, its raw pairing with the circle (`cx·ax + cy·ay + cz·az + cw·aw`)
 * vanishes, and a point on the circle projects to itself. A zero-radius circle is not
 * rejected: its self-pairing vanishes and the division floods the components.
 *
 * @receiver The point dropped onto the circle.
 * @param circle The circle projected onto.
 * @return The raw [RoundPoint] encoding the radial crossing pair.
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
 * The projection `dipole ∨ (a ∧ dipole^☆)`, normalized by the dipole's self-pairing
 * `dipole ∘ dipole` — its raw signed [Dipole.radius2], so an imaginary dipole negates every
 * component. The result is a genuine round point, not a null lift: the raw conformal
 * component sizes it by the pairing. A dipole with coincident endpoints is not rejected: its
 * self-pairing vanishes and the division floods the components.
 *
 * @receiver The point dropped onto the dipole.
 * @param dipole The dipole projected onto.
 * @return The raw [RoundPoint] on the carrier between the endpoints.
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
 * Returns the dipole where the line cuts the circle through this dipole's endpoints
 * centered on it.
 *
 * The projection `line ∨ (d ∧ line^☆)`, normalized by the line's self-pairing `line ∘ line`.
 * The result lies on the line — its carrier is the line itself, scaled by the carriers'
 * alignment — and preserves the conformal size: its `radius2` is the endpoint circle's, not
 * the bare foot separation. A dipole already on the line projects to itself. A line at
 * infinity has a zero self-pairing: the division floods the blade and the constructor
 * rejects it.
 *
 * @receiver The dipole dropped onto the line.
 * @param line The line projected onto.
 * @return The raw [Dipole] cut from the endpoint circle by the line.
 * @throws DegenerateDipoleException When the dipole's carrier is perpendicular to the line
 *   at the `SCALE` quantum — both endpoints share one foot, carried as the payload.
 * @throws NonBladeDipoleException When the line's self-pairing is zero — the flooded blade
 *   fails the constructor's check.
 */
public infix fun Dipole.project(line: Line): Dipole {
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
 * Returns the dipole where the circle meets the circle through this dipole's endpoints
 * orthogonal to it.
 *
 * The projection `circle ∨ (d ∧ circle^☆)`, normalized by the circle's self-pairing
 * `circle ∘ circle` — its raw signed `radius2`, so an imaginary circle negates every
 * component. A dipole whose endpoints already lie on the circle projects to itself. A
 * circle of exactly zero radius has a zero self-pairing: the division floods the blade and
 * the constructor rejects it.
 *
 * @receiver The dipole dropped onto the circle.
 * @param circle The circle projected onto.
 * @return The raw [Dipole] where the circle meets the endpoint circle.
 * @throws DegenerateDipoleException When the whole blade collapses at the `SCALE` quantum;
 *   carries the flat payload.
 * @throws NonBladeDipoleException When the circle's self-pairing is zero — the flooded
 *   blade fails the constructor's check.
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
