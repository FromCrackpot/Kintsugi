package fromcrackpot.kintsugi.gameengine.entity.traits

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction

/**
 * The trait of carrying a velocity that persists from step to step until something changes it.
 *
 * - The engine attaches no behavior: reading, integrating, and rewriting [velocity] is the game's code.
 */
public interface Inertial {

    /** The current velocity as a free vector [Direction], in CGA units per second. */
    public var velocity: Direction
}
