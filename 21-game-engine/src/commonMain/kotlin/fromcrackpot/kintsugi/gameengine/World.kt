package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/**
 * The base of every game's state: the world owns its entities and nodes as plain properties,
 * advances through [update], and is drawn as [root].
 *
 * Immediate-mode: the game declares its state as ordinary `val`s, `var`s, and lists — an
 * entity moves by rebinding (`ball *= motion`), leaves a list by removal — and [scene]
 * projects whatever currently exists into draw order. Nothing is registered with the engine
 * and nothing is retained between reads: [root] is recomputed from [scene] on every read.
 *
 * @constructor Builds the world around its fixed extent; the subclass's property initializers
 *   declare everything else.
 */
public abstract class World(
    /** The world's extent: the viewport [scene] carries as its bounds, fixed at construction. */
    public val bounds: SceneBounds,
) {

    /**
     * Advances the world by one step.
     *
     * The game loop drives this — once per whole base step and once for each frame's remainder
     * — with the current [Step] as context: the step's duration and sampled input read as
     * `step.dt` and `step.input`. The duration varies per call and a duration of zero must be
     * harmless. Game logic must be a function of `step.dt` — rates per second, never counts of
     * calls. The world mutates its own state in place; nothing is returned. A step may end in
     * [restart] instead: the world is then discarded and a new one built in its place, so no
     * state written before that call survives.
     */
    context(step: Step)
    public abstract fun update()

    /**
     * The world's current content as a tree in draw order — the projection [root] reads.
     *
     * The subclass assembles it from its own state on every read (`get() = GroupNode(arena,
     * bricks, ball)`), so whatever the properties hold right now is what draws: no
     * registration, no re-declaration after a change. Nested groups nest sub-trees; a group
     * entity enters as one child like any other node.
     */
    protected abstract val scene: GroupNode

    /**
     * The world drawn as a scene: [scene]'s children under [bounds].
     *
     * A pure projection recomputed on every read — nothing is retained between reads.
     */
    public val root: GroupNode
        get() = GroupNode(scene.children, bounds)
}

/**
 * One step of the simulation: the step's duration paired with the input sampled for it.
 *
 * Established as the context of each advancement the game loop drives — a function declaring
 * `context(step: Step)` reads the current step's `dt` and `input` without threading them as
 * arguments, and can only be called while a step is underway.
 */
public class Step(
    /** The step's duration in seconds. */
    public val dt: Double,
    /** The input sampled for this step. */
    public val input: Input,
)

/**
 * The signal that ends a step and the world it ran in: the world is discarded and a new one
 * is built in its place.
 *
 * Thrown by [restart] and caught by whatever drives the step — never by game code: a catch of
 * `Throwable` inside the game's step function would swallow it, and the world would live on.
 * It carries nothing; the replacement is built the way the first world was.
 */
public class Restart : Throwable()

/**
 * Ends the step and the world it runs in: the world is discarded and a fresh one is built in
 * its place, as at launch.
 *
 * Throws [Restart], which unwinds the step from any depth — a handler lambda included — so
 * nothing after the call runs and no state written before it survives; whatever drives the
 * step catches the signal and builds the replacement. Callable only while a [Step] is
 * underway — the context is the gate, and nothing of it is read.
 *
 * @throws Restart Always — the signal the step's driver catches.
 */
context(_: Step)
public fun restart(): Nothing = throw Restart()
