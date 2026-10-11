package fromcrackpot.kintsugi.gameengine.traits

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction

/**
 * The trait of carrying a velocity.
 *
 * A trait interface an entity opts into. The engine attaches no behavior to it — reading
 * [velocity], integrating it over a step, and rewriting it are the game's own update code.
 */
public interface Kinetic {

    /** The current velocity as a free vector — a [Direction], in CGA units per second. */
    public var velocity: Direction
}
