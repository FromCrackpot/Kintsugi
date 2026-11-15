package fromcrackpot.kintsugi.cga.d2.operations.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
// Upward reach by design. The `d2/` tiers are a reading order, not a dependency DAG.
import fromcrackpot.kintsugi.cga.d2.operations.queries.predicates.on

/*
LINE
 */

/**
 * `Line meet Line => FlatPoint`<br>
 * Returns the point where the two lines cross. Parallel lines meet at infinity, along their shared
 * direction.
 *
 * - Coincident lines give the all-zero flat point, not an error.
 *
 * @receiver The first line.
 * @param other The second line.
 * @return The raw crossing point.
 */
public infix fun Line.meet(other: Line): FlatPoint = FlatPoint(
    px = this.gz * other.gy - this.gy * other.gz, // e41
    py = this.gx * other.gz - this.gz * other.gx, // e42
    pz = this.gy * other.gx - this.gx * other.gy  // e43
)

/**
 * `Line meet Circle => Dipole`<br>
 * Returns the dipole of the two points where the line crosses the circle. It is imaginary when the
 * line misses.
 *
 * - Tangency gives a zero-radius dipole centered at the tangent point, never an error.
 * - `Circle meet Line` negates every component. The crossing points stay, the orientation flips.
 *
 * @receiver The line.
 * @param circle The circle.
 * @return The raw dipole with the crossing points as endpoints.
 * @throws DegenerateDipoleException when this is the all-zero line.
 */
public infix fun Line.meet(circle: Circle): Dipole = Dipole(
    gx = this.gx * circle.cw, // e23
    gy = this.gy * circle.cw, // e31
    gz = this.gz * circle.cw, // e12

    px = this.gz * circle.cy - this.gy * circle.cz, // e41
    py = this.gx * circle.cz - this.gz * circle.cx, // e42
    pz = this.gy * circle.cx - this.gx * circle.cy  // e43
)

/**
 * `Line meet Dipole => RoundPoint`<br>
 * Returns the point where the line crosses the dipole's carrier.
 *
 * - The result is sized: real between the dipole's endpoints, zero-radius at one, imaginary beyond.
 * - Parallel carriers give a round point at infinity.
 *
 * @receiver The line.
 * @param dipole The dipole whose carrier is crossed.
 * @return The raw round point at the crossing.
 */
public infix fun Line.meet(dipole: Dipole): RoundPoint = RoundPoint(
    ax = this.gz * dipole.gy - this.gy * dipole.gz,                            // e1
    ay = this.gx * dipole.gz - this.gz * dipole.gx,                            // e2
    az = this.gy * dipole.gx - this.gx * dipole.gy,                            // e3
    aw = -(this.gx * dipole.px + this.gy * dipole.py + this.gz * dipole.pz),   // e4
)

/**
 * `Line meet Segment => RoundPoint?`<br>
 * Returns the point where the line crosses the segment, or `null` when it misses.
 *
 * - A parallel or collinear line answers `null`.
 *
 * @receiver The line.
 * @param segment The segment.
 * @return The raw crossing point, or `null` when none lies within the segment, endpoints included.
 */
public infix fun Line.meet(segment: Segment): RoundPoint? =
    // `!isAtInfinity` is not redundant with `!isImaginary`. Parallel carriers land imaginary, but a
    // collinear line zeroes every component (radius² = 0) and would pass the realness gate alone.
    (this meet segment.toDipole).takeIf { !it.isAtInfinity && !it.isImaginary }

/*
CIRCLE
 */

/**
 * `Circle meet Line => Dipole`<br>
 * Returns the dipole of the two points where the circle crosses the line. It is imaginary when
 * they miss.
 *
 * - Negates `Line meet Circle`. The crossing points stay, the orientation flips.
 *
 * @receiver The circle.
 * @param line The line.
 * @return The raw dipole with the crossing points as endpoints.
 * @throws DegenerateDipoleException when [line] is all-zero.
 */
public infix fun Circle.meet(line: Line): Dipole = Dipole(
    gx = -this.cw * line.gx, // e23
    gy = -this.cw * line.gy, // e31
    gz = -this.cw * line.gz, // e12

    px = this.cz * line.gy - this.cy * line.gz, // e41
    py = this.cx * line.gz - this.cz * line.gx, // e42
    pz = this.cy * line.gx - this.cx * line.gy  // e43
)

/**
 * `Circle meet Circle => Dipole`<br>
 * Returns the dipole of the two points where the circles cross. It is imaginary when they don't
 * touch.
 *
 * - Tangent circles give a zero-radius dipole. Concentric ones give a raw dipole at infinity.
 * - Swapping the operands negates every component. The points stay, the orientation flips.
 *
 * @receiver The first circle.
 * @param other The second circle.
 * @return The raw dipole with the crossing points as endpoints.
 * @throws DegenerateDipoleException when the circles coincide, in either orientation.
 */
public infix fun Circle.meet(other: Circle): Dipole = Dipole(
    gx = this.cx * other.cw - this.cw * other.cx, // e23
    gy = this.cy * other.cw - this.cw * other.cy, // e31
    gz = this.cz * other.cw - this.cw * other.cz, // e12

    px = this.cz * other.cy - this.cy * other.cz, // e41
    py = this.cx * other.cz - this.cz * other.cx, // e42
    pz = this.cy * other.cx - this.cx * other.cy  // e43
)

/*
SEGMENT
 */

/**
 * `Segment meet Line => RoundPoint?`<br>
 * Returns the point where the line crosses the segment, or `null` when it misses.
 *
 * @receiver The segment.
 * @param line The line.
 * @return The raw crossing point, or `null`.
 */
public infix fun Segment.meet(line: Line): RoundPoint? = line meet this

/**
 * `Segment meet Segment => RoundPoint?`<br>
 * Returns the point where the two segments cross, or `null` when they don't.
 *
 * - A collinear overlap answers `null`, since it is not a point.
 *
 * @receiver The first segment.
 * @param other The second segment.
 * @return The raw crossing point, or `null` when none lies within both segments, endpoints included.
 */
public infix fun Segment.meet(other: Segment): RoundPoint? =
    // The meet's realness encodes bounds wrt `other`'s dipole only, so the receiver's side
    // needs its own `on` probe.
    (this.carrier meet other)?.takeIf { it on this }
