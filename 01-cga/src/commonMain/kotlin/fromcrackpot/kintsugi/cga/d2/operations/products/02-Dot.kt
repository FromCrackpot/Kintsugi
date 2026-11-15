package fromcrackpot.kintsugi.cga.d2.operations.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint

/*
FLAT POINT
 */

/**
 * `FlatPoint dot FlatPoint => Double`<br>
 * Returns the raw inner product of two flat points. It is `−½·d²` for finite points `d` apart, so
 * zero when they coincide.
 *
 * - At infinity there is no distance reading. The value is the plain dot of the carried vectors.
 *
 * @receiver The left flat point.
 * @param other The right flat point.
 * @return The raw inner product.
 */
public infix fun FlatPoint.dot(other: FlatPoint): Double {
    // a·b = −½‖a−b‖², evaluated difference-first: the error scales with the separation.
    // Not via toRound: its lift aw = (px²+py²)/2 loses O(coord²·ε) to cancellation first.
    if (this.isAtInfinity || other.isAtInfinity)
        return this.px * other.px + this.py * other.py + 0.0

    val dx = this.px - other.px
    val dy = this.py - other.py
    return -0.5 * (dx * dx + dy * dy) + 0.0
}

/**
 * `FlatPoint dot Line => Double`<br>
 * Returns the line's equation evaluated at the point. The result is the raw signed distance to
 * the line, zero on it.
 *
 * - Positive on the side the line's normal points toward.
 * - A direction at infinity dots with the normal instead. The result is zero when it runs along the line.
 *
 * @receiver The point to evaluate at.
 * @param line The line whose equation is evaluated.
 * @return The raw signed value.
 */
public infix fun FlatPoint.dot(line: Line): Double =
    // F ∨ L (grades 2+3 → 1) = −(px·gx + py·gy + pz·gz)·e4, the only component the
    // right-complement pairing keeps. Returned negated.
    this.px * line.gx + this.py * line.gy + this.pz * line.gz

/**
 * `FlatPoint dot Circle => Double`<br>
 * Returns the raw probe `½·(r² − d²)` of the point against a classic circle: positive inside,
 * zero on the circle, negative outside.
 *
 * - An inside-out circle flips the sign. An imaginary classic circle probes negative everywhere.
 *
 * @receiver The point to probe with.
 * @param circle The circle probed against.
 * @return The raw probe value.
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
 * Returns the raw inner product of two round points. It is `−½·d²` for points `d` apart, so zero
 * when two finite positions coincide.
 *
 * - The `−½·d²` reading holds for plain lifted points. A point carrying its own `aw` shifts the value.
 *
 * @receiver The left round point.
 * @param other The right round point.
 * @return The raw inner product.
 */
public infix fun RoundPoint.dot(other: RoundPoint): Double =
    this.ax * other.ax + this.ay * other.ay - this.az * other.aw - this.aw * other.az

/**
 * `RoundPoint dot Line => Double`<br>
 * Returns the line's equation evaluated at the point. The result is the raw signed distance to
 * the line, zero on it.
 *
 * - Positive on the side the line's normal points toward.
 * - A direction at infinity dots with the normal instead. The result is zero when it runs along the line.
 *
 * @receiver The point to evaluate at.
 * @param line The line whose equation is evaluated.
 * @return The raw signed value.
 */
public infix fun RoundPoint.dot(line: Line): Double =
    // Antiwedge R ∨ L (grades 1+3 = n) under the same right-complement pairing as
    // RoundPoint ∨ Circle.
    this.ax * line.gx + this.ay * line.gy + this.az * line.gz

/**
 * `RoundPoint dot Circle => Double`<br>
 * Returns the raw probe `½·(r² − d²)` of the point against a classic circle: positive inside,
 * zero on the circle, negative outside.
 *
 * - An inside-out circle flips the sign. An imaginary classic circle probes negative everywhere.
 *
 * @receiver The point to probe with.
 * @param circle The circle probed against.
 * @return The raw probe value.
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
 * Returns the raw inner product of two circles. It is zero exactly when they cross at right angles.
 *
 * - Flipping one operand inside-out negates the value.
 *
 * @receiver The left circle.
 * @param other The right circle.
 * @return The raw inner product, `½·(d² − r₁² − r₂²)` for classic circles `d` apart.
 */
public infix fun Circle.dot(other: Circle): Double =
    // Strict grade-3 𝐆 dot: e423·e423 = e431·e431 = −1, e321↔e412 = +1.
    -(this.cx * other.cx + this.cy * other.cy) + this.cz * other.cw + this.cw * other.cz
