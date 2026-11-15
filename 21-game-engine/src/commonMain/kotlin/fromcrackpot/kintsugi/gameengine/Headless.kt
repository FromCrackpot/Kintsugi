package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerType
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.gameengine.input.Keys
import fromcrackpot.kintsugi.gameengine.input.Pointer
import kotlin.math.roundToInt

/* CONTROLS: the level-state vocabulary a headless session speaks */

/**
 * One step's commanded input, stated as level state only: which keys are held, and where the
 * pointer is.
 *
 * - [drive] derives press and release edges by diffing consecutive steps' controls.
 */
public class Controls(
    /** The keys held down during the step. */
    public val keys: Set<Key> = emptySet(),
    /** Where the pointer is, as a [FlatPoint] in CGA model units, or `null` when there is no pointer. */
    public val pointer: FlatPoint? = null,
    /** Whether the pointer is pressed. Meaningful only while [pointer][Controls.pointer] is non-null. */
    public val pointerDown: Boolean = false,
    /** The pointer's device: `PointerType.Mouse` by default, `PointerType.Touch` for a finger. */
    public val pointerType: PointerType = PointerType.Mouse,
) {
    public companion object {
        /** The neutral controls: no keys held and no pointer, for a step with no input at all. */
        public val NONE: Controls = Controls()
    }
}

/* DRIVER: a game session without a surface, clock or user */

/**
 * Runs a whole game session headlessly. The world [world] builds advances through [seconds]
 * seconds of fixed-rate steps, its input commanded by [controls].
 *
 * - Deterministic: every step is exactly `1/tickRate`. [controls] runs before each step, [onStep] after.
 * - Edges come from diffing consecutive controls: one press per hold, as in a live game.
 * - A [restart] rebuilds the world within the same step. The step count is unchanged.
 *
 * @param W The world type driven.
 * @param world Builds the world to advance, at the session's start and again on each restart.
 * @param seconds The session length. The step count is `seconds * tickRate`, rounded to nearest.
 * @param tickRate The step frequency in Hz. Every step's `dt` is `1/tickRate`.
 * @param onStep Observation hook run after each step. Does nothing by default.
 * @param controls The input policy evaluated before each step, [Controls.NONE] by default.
 * @return The world live after the last step: the last one [world] built.
 */
public fun <W : World> drive(
    world: () -> W,
    seconds: Double,
    tickRate: Int = 120,
    onStep: (elapsed: Double, world: W) -> Unit = { _, _ -> },
    controls: (elapsed: Double, world: W) -> Controls = { _, _ -> Controls.NONE },
): W {
    val dt = 1.0 / tickRate
    val steps = (seconds * tickRate).roundToInt()
    var w = world()
    var previous = Controls.NONE
    repeat(steps) { index ->
        val current = controls(index * dt, w)
        try {
            w.step(dt, synthesize(previous, current))
        } catch (_: Restart) {
            w = world()
        }
        previous = current
        onStep((index + 1) * dt, w)
    }
    return w
}

/** Fabricates the step's [Input] from the level-state diff. Every step consumes its own edges. */
private fun synthesize(previous: Controls, current: Controls): Input {
    val previousDown = previous.pointer != null && previous.pointerDown
    val currentDown = current.pointer != null && current.pointerDown
    val previousPointer = previous.pointer
    val pointer = when {
        current.pointer != null -> Pointer(
            position = current.pointer,
            isDown = currentDown,
            wasPressed = currentDown && !previousDown,
            wasReleased = !currentDown && previousDown,
            type = current.pointerType,
        )
        previousPointer != null && previousDown ->
            Pointer(previousPointer, isDown = false, wasPressed = false, wasReleased = true, previous.pointerType)
        else -> null
    }
    return Input(
        pointers = listOfNotNull(pointer),
        keys = Keys(
            down = current.keys,
            pressed = current.keys - previous.keys,
            released = previous.keys - current.keys,
        ),
    )
}
