package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import kotlin.math.roundToInt

/* CONTROLS — the level-state vocabulary a headless session speaks */

/**
 * One step's commanded input, stated as level state only: which keys are held, and where the
 * pointer is.
 *
 * There are no edge flags to manage — [drive] derives press and release edges by comparing each
 * step's controls against the previous step's, so a script states only what is held and the
 * latching contract falls out correctly by construction.
 */
public class Controls(
    /** The keys held down during the step. */
    public val keys: Set<Key> = emptySet(),
    /** Where the pointer is, as a [FlatPoint] in CGA model units, or `null` when there is no pointer. */
    public val pointer: FlatPoint? = null,
    /** Whether the pointer is pressed — meaningful only while [pointer] is non-null. */
    public val pointerDown: Boolean = false,
) {
    public companion object {
        /** The neutral controls: no keys held, no pointer — a step with no input at all. */
        public val NONE: Controls = Controls()
    }
}

/* DRIVER — a game session without a surface, clock, or user */

/**
 * Advances the world [world] builds through [seconds] seconds of fixed-rate steps, its input
 * commanded by [controls] — a whole game session run headlessly.
 *
 * Every step advances by exactly `1/tickRate` seconds, so a session is deterministic: no wall
 * clock, no display frames, no remainder steps. [controls] is evaluated once per step, before
 * the step runs, with the elapsed seconds at the step's start and the world itself — a
 * time-scripted session ignores the world, a closed-loop one reads it. Edges are synthesized by
 * diffing consecutive controls: a key present that was absent the step before reads as a press
 * edge on exactly that step, a key gone as a release edge, matching the sampled-input latching
 * contract — one press per hold, never one per step. The pointer follows the same rule through
 * [Controls.pointerDown], and a pointer that vanishes while down yields its release edge at its
 * last position. The first step diffs against [Controls.NONE], so state held from the very
 * start still edges.
 *
 * [onStep] runs after every step with the elapsed seconds at the step's end — the observation
 * hook a session records itself through. A step ending in [restart] discards the world: [world]
 * builds the replacement within that same step, [onStep] observes the replacement from that
 * step on, and the next step advances it — the step count is unchanged. Controls diff across
 * the swap as across any two steps, so a key held through a restart is still down in the
 * replacement without a fresh press edge. An exception thrown by [controls], [onStep], or the
 * world's update propagates unchanged, ending the session — [Restart] is the one the driver
 * consumes.
 *
 * @param W The world type driven.
 * @param world Builds the world to advance — at the session's start, and again on each restart.
 * @param seconds The session length; the step count is `seconds * tickRate`, rounded to nearest.
 * @param tickRate The step frequency in Hz; every step's `dt` is `1/tickRate`.
 * @param onStep Observation hook run after each step; defaults to doing nothing.
 * @param controls The input policy evaluated before each step; defaults to [Controls.NONE].
 * @return The world live after the last step — the last one [world] built.
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
            context(Step(dt, synthesize(previous, current))) { w.update() }
        } catch (_: Restart) {
            w = world()
        }
        previous = current
        onStep((index + 1) * dt, w)
    }
    return w
}

/** Fabricates the step's [Input] from the level-state diff — every step consumes its own edges. */
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
        )
        previousPointer != null && previousDown ->
            Pointer(previousPointer, isDown = false, wasPressed = false, wasReleased = true)
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
