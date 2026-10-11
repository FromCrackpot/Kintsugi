package fromcrackpot.kintsugi.cga.d2.queries.measures

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import kotlin.math.atan2


/*
LINE
 */

/**
 * `Line angleTo Line => Angle`<br>
 * Returns the signed angle turning this line's direction onto [other]'s — counter-clockwise
 * positive, in `(−π, π]`.
 *
 * The cosine is the lines' weight contraction `a ∨ b^☆` and the sine the raw weight of their
 * crossing point `a ∨ b` — for unit normals `(gx, gy)`, the normals' dot and cross products —
 * combined by `atan2`, so the answer is well-behaved at every angle. The result is raw at
 * full `Double` precision. Equal directions answer the zero angle, and reversing either
 * line's orientation offsets the answer by a half turn; the unoriented angle between the
 * carriers is `min(|φ|, π − |φ|)`. A line at infinity —
 * zero normal — has no direction, and the answer degenerates to the zero angle; gate with
 * [Line.isAtInfinity].
 *
 * @receiver The line the angle turns from.
 * @param other The line the angle turns onto.
 * @return The raw signed [Angle] in `(−π, π]`.
 */
public infix fun Line.angleTo(other: Line): Angle = Angle(
    atan2(
        this.gx * other.gy - this.gy * other.gx + 0.0,
        this.gx * other.gx + this.gy * other.gy + 0.0
    )
)
