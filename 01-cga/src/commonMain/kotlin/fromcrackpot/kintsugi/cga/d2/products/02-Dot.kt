package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint

/*
FLAT POINT
 */

/**
 * `FlatPoint dot FlatPoint => Double`<br>
 * Returns the raw inner product of two flat points — zero when they coincide, `−½·d²` for
 * finite points `d` apart.
 *
 * The negated half squared Euclidean distance, at full `Double` precision. If either operand
 * is at infinity the distance reading is gone — the value degenerates to the plain dot of the
 * two carried vectors.
 *
 * @receiver The left flat point.
 * @param other The right flat point.
 * @return The raw inner product; `−½·d²` when both points are finite.
 */
public infix fun FlatPoint.dot(other: FlatPoint): Double {
    // Difference-first evaluation of the conformal identity a·b = −½‖a−b‖². Deliberately not
    // delegated via toRound: the aw = (px²+py²)/2 lift loses sub-ULP detail to O(coord²·ε)
    // cancellation before any dot can run, whereas the difference-first error scales with the
    // separation, not the coordinate magnitude.
    if (this.isAtInfinity || other.isAtInfinity)
        return this.px * other.px + this.py * other.py + 0.0

    val dx = this.px - other.px
    val dy = this.py - other.py
    return -0.5 * (dx * dx + dy * dy) + 0.0
}

/**
 * `FlatPoint dot Line => Double`<br>
 * Returns the line's equation evaluated at the point: zero when the point lies on the line,
 * the raw signed distance to it otherwise.
 *
 * The raw value `px·gx + py·gy + pz·gz` — the classical `n·p + d` form, derived from the
 * antiwedge `F ∨ L`. For a finite point and a finite line the magnitude is the Euclidean
 * distance and the sign is positive on the side the line's normal `(gx, gy)` points toward.
 * A direction (at infinity) dots with the normal instead: zero when it runs along the line.
 *
 * @receiver The point to evaluate at.
 * @param line The line whose equation is evaluated.
 * @return The raw signed value; the signed Euclidean distance for finite operands.
 */
public infix fun FlatPoint.dot(line: Line): Double =
    // Antiwedge-derived: F ∨ L (grades 2+3 → grade 1) is −(px·gx + py·gy + pz·gz)·e4 — only
    // the e4 component survives the right-complement pairing; returned negated.
    this.px * line.gx + this.py * line.gy + this.pz * line.gz

/**
 * `FlatPoint dot Circle => Double`<br>
 * Returns the raw point-against-circle probe: positive inside, zero on the circle, negative
 * outside, for a finite point and a classic circle.
 *
 * The value `½·(r² − d²)` — half the negated power of the point with respect to the circle,
 * `d` its distance to the center. The identity takes `r²` signed, so an imaginary classic
 * circle probes negative everywhere — no point is inside. An inside-out circle negates every
 * component and so negates the value. Evaluated on the conformal lift; a point at infinity
 * has no lift and the value degenerates to a directional pairing with no distance reading.
 *
 * @receiver The point to probe with.
 * @param circle The circle probed against.
 * @return The raw probe value; `½·(r² − d²)` for a finite point and a classic circle.
 */
public infix fun FlatPoint.dot(circle: Circle): Double =
    // Lift via toRound: the strict grade-2 𝐆 dot is the constant −pz1·pz2 (no distance info)
    // and F ∨ C isn't a scalar.
    this.toRound dot circle

/*
ROUND POINT
 */

/**
 * `RoundPoint dot RoundPoint => Double`<br>
 * Returns the raw inner product of two round points — zero when two finite positions
 * coincide, `−½·d²` when they sit `d` apart.
 *
 * The strict inner product under the conformal metric; the cross terms carry the null
 * pairing `e3·e4 = −1`. The `−½·d²` reading holds for
 * two null lifts; an operand carrying a semantic round bulk `aw` (a center, an attitude)
 * feeds that data into the same bilinear form instead.
 *
 * @receiver The left round point.
 * @param other The right round point.
 * @return The raw inner product; `−½·d²` for two finite null lifts.
 */
public infix fun RoundPoint.dot(other: RoundPoint): Double =
    this.ax * other.ax + this.ay * other.ay - this.az * other.aw - this.aw * other.az

/**
 * `RoundPoint dot Line => Double`<br>
 * Returns the line's equation evaluated at the point: zero when the point lies on the line,
 * the raw signed distance to it otherwise.
 *
 * The raw value `ax·gx + ay·gy + az·gz` — the flat-point form with [RoundPoint.az] in the
 * weight role; the round bulk `aw` plays no part (a line has no `e321` component). Sign and
 * at-infinity behavior match `FlatPoint dot Line`: positive on the normal's side, and a
 * direction dots with the normal.
 *
 * @receiver The point to evaluate at.
 * @param line The line whose equation is evaluated.
 * @return The raw signed value; the signed Euclidean distance for finite operands.
 */
public infix fun RoundPoint.dot(line: Line): Double =
    // Antiwedge R ∨ L (grades 1+3 = n) under the same right-complement pairing as
    // RoundPoint ∨ Circle.
    this.ax * line.gx + this.ay * line.gy + this.az * line.gz

/**
 * `RoundPoint dot Circle => Double`<br>
 * Returns the raw point-against-circle probe: positive inside, zero on the circle, negative
 * outside, for a finite point and a classic circle.
 *
 * The value `½·(r² − d²)` — half the negated power of the point with respect to the circle.
 * The identity takes `r²` signed, so an imaginary classic circle probes negative everywhere —
 * no point is inside. Derived from the antiwedge `R ∨ C`: the full bilinear pairing
 * `cx·ax + cy·ay + cz·az + cw·aw`, so an operand carrying a semantic round bulk `aw` feeds
 * that data into the pairing. An inside-out circle negates the value.
 *
 * @receiver The point to probe with.
 * @param circle The circle probed against.
 * @return The raw probe value; `½·(r² − d²)` for a finite point and a classic circle.
 */
public infix fun RoundPoint.dot(circle: Circle): Double =
    // Antiwedge R ∨ C (grades 1+3 = n, so the strict 𝐆 dot is zero): right-complement pairing
    // e1↔e423, e2↔e431, e3↔e412, e4↔e321 with all-positive signs.
    circle.cx * this.ax + circle.cy * this.ay + circle.cz * this.az + circle.cw * this.aw

/*
CIRCLE
 */

/**
 * `Circle dot Circle => Double`<br>
 * Returns the raw inner product of two circles — zero exactly when they cross at right
 * angles.
 *
 * The strict inner product: `½·(d² − r₁² − r₂²)` for two classic circles with centers `d`
 * apart, which vanishes precisely on orthogonal circles and gives `−r²` for a circle dotted
 * with itself. Both `r²` are taken signed, so the identity extends to imaginary operands.
 * Flipping one operand inside-out negates the value.
 *
 * @receiver The left circle.
 * @param other The right circle.
 * @return The raw inner product; `½·(d² − r₁² − r₂²)` for two classic circles.
 */
public infix fun Circle.dot(other: Circle): Double =
    // Strict grade-3 𝐆 dot: e423·e423 = e431·e431 = −1, e321↔e412 = +1.
    -(this.cx * other.cx + this.cy * other.cy) + this.cz * other.cw + this.cw * other.cz
