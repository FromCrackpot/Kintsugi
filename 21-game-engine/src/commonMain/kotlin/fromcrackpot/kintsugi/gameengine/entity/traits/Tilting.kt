package fromcrackpot.kintsugi.gameengine.entity.traits

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.gameengine.entity.kinematics.Kinematics
import fromcrackpot.kintsugi.gameengine.entity.kinematics.TiltServo

/**
 * The trait of tilting about the centroid to a target angle and back to rest, driven by the engine.
 *
 * - Set [tiltTarget] to tilt. The entity turns there at [tiltRate], the target resets to zero, and it turns back.
 * - Expects a polygon whose first edge runs along +X at rest, as every `Polygon.Rectangle` does.
 * - The tilt owns the entity's orientation. The class carries [tiltTarget] through its own rebuild.
 */
public interface Tilting : Kinematic {

    /** The tilt to reach before coming back to rest. Zero at rest and once reached. */
    public var tiltTarget: Angle

    /** How fast the tilt changes, per second, on the way out and back alike. */
    public val tiltRate: Angle

    /** The tilt servo. */
    override val law: Kinematics get() = TiltServo
}

/**
 * The entity's current tilt: the angle its first edge makes with +X, counter-clockwise positive.
 *
 * @receiver The tilting polygon.
 * @param E The receiver's type.
 */
public val <E> E.tilt: Angle where E : Polygon, E : Tilting
    get() = edges[0].attitude.angle
