package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.gameengine.entity.traits.Kinematic
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The base of every game's state: the world holds its nodes in [staged] properties, advances
 * through [step], and is drawn as [root].
 *
 * - Staged properties read and write like plain ones. Their staging order is the default draw order.
 *
 * @constructor Builds the world around its fixed extent. The subclass's property initializers
 *   stage everything else.
 */
public abstract class World(
    /** The world's extent: the viewport [scene] carries as its bounds, fixed at construction. */
    public val bounds: SceneBounds,
) {

    private val stage = mutableListOf<Staged<*>>()
    internal var overlay: GroupNode = GroupNode()
    private val playing = mutableListOf<Sound>()

    /** The sounds the latest step played, in play order: empty before the first step, kept when a step [restart]s. */
    public val played: List<Sound> get() = playing.toList()

    internal fun record(sound: Sound) {
        playing += sound
    }

    /**
     * Returns a property delegate holding [initial] on the world's stage:
     * `var paddle by staged(Paddle(node))`.
     *
     * - The default [scene] draws the staged nodes in the order their properties were staged.
     *
     * @param N The staged node's type.
     * @param initial The node the property holds at first.
     * @return The delegate.
     */
    protected fun <N : Node> staged(initial: N): ReadWriteProperty<World, N> = Staged(initial).also { stage += it }

    /**
     * Advances the world by one step of [dt] seconds, with [input] sampled for it.
     *
     * - First every staged [Kinematic] node, group members included, and every explosion moves. Then the update runs.
     * - `dt` may be zero. A step ending in [restart] throws, and the world is to be discarded.
     *
     * @param dt The step's duration in seconds.
     * @param input The input sampled for the step, none by default.
     * @throws Restart when the step ends in [restart].
     */
    public fun step(dt: Double, input: Input = Input.NONE): Unit = context(Step(dt, input)) {
        playing.clear()
        for (staged in stage) staged.advance()
        overlay = advanced(overlay)
        update()
    }

    /**
     * Advances the game's own state by one step, reading `step.dt` and `step.input` from the
     * current [Step].
     *
     * - `dt` varies per call and may be zero. Write logic per second, never per call.
     * - A step may end in [restart]. The world is then discarded, and nothing written before survives.
     */
    context(step: Step)
    protected abstract fun update()

    /**
     * The world's current content as a tree in draw order, the projection [root] reads.
     *
     * - By default the staged nodes in staging order; override it when draw order differs.
     */
    protected open val scene: GroupNode
        get() = GroupNode(stage.map { it.value })

    /**
     * The world drawn as a scene: [scene]'s children, then what the engine draws over them, such as explosions
     * in flight, under [bounds]. Recomputed on every read.
     */
    public val root: GroupNode
        get() {
            val drawn = scene.children
            return GroupNode(if (overlay.children.isEmpty()) drawn else drawn + overlay, bounds)
        }
}

private class Staged<N : Node>(var value: N) : ReadWriteProperty<World, N> {

    context(_: Step)
    fun advance() {
        value = advanced(value)
    }

    override fun getValue(thisRef: World, property: KProperty<*>): N = value

    override fun setValue(thisRef: World, property: KProperty<*>, value: N) {
        this.value = value
    }
}

// Bottom-up: a group is rebuilt only when a member moved, then a kinematic node moves itself.
context(_: Step)
private fun <N : Node> advanced(node: N): N {
    val group = node as? GroupNode
    val walked: N = if (group != null) {
        val members = group.children.map { advanced(it) }
        if (members.indices.all { members[it] === group.children[it] }) node else {
            val copied = group.copy(children = members)
            check(copied::class == node::class) {
                "${node::class.simpleName}.copy returned ${copied::class.simpleName}, not its own class"
            }
            @Suppress("UNCHECKED_CAST")
            copied as N
        }
    } else node
    // An explicit cast, not a smart cast to `N & Kinematic`: that one miscompiles on wasmJs (invalid module).
    val kinematic = walked as? Kinematic ?: return walked
    return kinematic.law.advance(walked)
}

/**
 * One step of the simulation: the step's duration paired with the input sampled for it.
 *
 * - A function declaring `context(step: Step)` reads `dt` and `input`, and runs only during a step.
 */
public class Step(
    /** The step's duration in seconds. */
    public val dt: Double,
    /** The input sampled for this step. */
    public val input: Input,
)

/**
 * The signal that ends a step and the world it ran in, thrown by [restart].
 *
 * - Never catch it: a `catch (Throwable)` in the step function swallows it, and the world lives on.
 */
public class Restart : Throwable()

/**
 * Ends the step and the world it runs in: the world is discarded and a fresh one is built in
 * its place, as at launch.
 *
 * - Unwinds the step from any depth, handler lambdas included: nothing after the call runs.
 * - Callable only while a [Step] is underway.
 *
 * @throws Restart always, as the signal the step's driver catches.
 */
context(_: Step)
public fun restart(): Nothing = throw Restart()
