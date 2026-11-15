package fromcrackpot.kintsugi.gameengine.entity.traits

import fromcrackpot.kintsugi.gameengine.entity.kinematics.Kinematics

/**
 * The trait of moving under a law the engine advances every step: the mark of a kinematic trait.
 *
 * - A kinematic trait supplies its [law] by default. A class with two such traits picks one explicitly.
 */
public interface Kinematic {

    /** The law the engine advances the entity by, once per step. */
    public val law: Kinematics
}
