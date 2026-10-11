package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint

/* SNAPSHOTS — what the `state` lambda sees, once per step */

/**
 * One step's input snapshot: every active pointer and the keyboard, handed to the game's step
 * function — sampled state, never events.
 *
 * Edge flags ([Pointer.wasPressed], [Pointer.wasReleased], and the [Keys] pair) are latched
 * across frames and consumed by the first step run for a frame; later steps of the same frame
 * read level state only. Consequences: a click shorter than a frame cannot vanish, and a press
 * during a catch-up burst of steps fires exactly once, never once per step. Edges are booleans —
 * several presses within one frame read as one.
 *
 * Pointers unify mouse and touch: a mouse is one pointer that persists while up — hover — while
 * a lifted touch contact is dropped once its release edge has been consumed.
 */
public class Input internal constructor(
    /** The active pointers: a mouse always, touch contacts until their release is consumed. */
    public val pointers: List<Pointer>,
    /** The keyboard snapshot. */
    public val keys: Keys,
) {
    /** The first active pointer, or `null` when there is none — the one a single-pointer game reads. */
    public val pointer: Pointer? get() = pointers.firstOrNull()

    public companion object {
        /**
         * The neutral snapshot: no pointers, no keys down, no latched edges — the input of a
         * step driven without a user, e.g. a headless test.
         */
        public val NONE: Input = Input(emptyList(), Keys(emptySet(), emptySet(), emptySet()))

        /**
         * Builds an input snapshot from hand-fabricated state — keys and pointers stated
         * explicitly instead of sampled from a surface.
         *
         * The fabricated facts mean exactly what sampled ones do: [keysDown] is level state,
         * [keysPressed] and [keysReleased] are edges claimed to have landed since the last
         * edge-consuming step. The caller owns that contract — nothing checks an edge against
         * the level state. [NONE] is the no-input shorthand.
         *
         * @param pointers The active pointers, fabricated via `Pointer.of`.
         * @param keysDown The keys held down.
         * @param keysPressed The keys claimed as press edges.
         * @param keysReleased The keys claimed as release edges.
         * @return The snapshot, indistinguishable from a sampled one.
         */
        public fun of(
            pointers: List<Pointer> = emptyList(),
            keysDown: Set<Key> = emptySet(),
            keysPressed: Set<Key> = emptySet(),
            keysReleased: Set<Key> = emptySet(),
        ): Input = Input(pointers, Keys(keysDown, keysPressed, keysReleased))
    }
}

/**
 * One pointer — a mouse or a touch contact — in CGA model units.
 *
 * The position converts at the event boundary through the same fit-scale mapping the draw pass
 * uses, so game code never sees pixels. [isDown] is level state; [wasPressed] and [wasReleased]
 * are latched edges — [Input] carries the latching contract. All four properties are independent
 * facts: a complete click within one frame reads `isDown = false, wasPressed = true,
 * wasReleased = true`.
 */
public class Pointer internal constructor(
    /** Where the pointer is, as a [FlatPoint] in CGA model units. */
    public val position: FlatPoint,
    /** Whether the pointer is down — `false` for a hovering mouse. */
    public val isDown: Boolean,
    /** Whether a press landed since the last edge-consuming step — a latched edge. */
    public val wasPressed: Boolean,
    /** Whether a release landed since the last edge-consuming step — a latched edge. */
    public val wasReleased: Boolean,
) {
    public companion object {
        /**
         * Builds a pointer snapshot from hand-fabricated state — a sampled pointer's explicit
         * counterpart.
         *
         * The four facts stay independent, under the sampled contract: [isDown] is level state,
         * [wasPressed] and [wasReleased] are edges claimed to have landed since the last
         * edge-consuming step. The caller owns that contract — nothing checks an edge against
         * the level state.
         *
         * @param position Where the pointer is, as a [FlatPoint] in CGA model units.
         * @param isDown Whether the pointer is pressed.
         * @param wasPressed Whether a press edge is claimed.
         * @param wasReleased Whether a release edge is claimed.
         * @return The pointer snapshot, indistinguishable from a sampled one.
         */
        public fun of(
            position: FlatPoint,
            isDown: Boolean = false,
            wasPressed: Boolean = false,
            wasReleased: Boolean = false,
        ): Pointer = Pointer(position, isDown, wasPressed, wasReleased)
    }
}

/**
 * The keyboard snapshot, over Compose `Key` codes.
 *
 * [isDown] is level state; [wasPressed] and [wasReleased] are latched edges — [Input] carries
 * the latching contract. OS auto-repeat is filtered: holding a key yields one press edge, not a
 * stream.
 */
public class Keys internal constructor(
    private val down: Set<Key>,
    private val pressed: Set<Key>,
    private val released: Set<Key>,
) {
    /**
     * Returns whether [key] is held down — level state.
     *
     * @param key The Compose key code to test.
     * @return `true` while the key is held.
     */
    public fun isDown(key: Key): Boolean = key in down

    /**
     * Returns whether [key] went down since the last edge-consuming step — a latched edge.
     *
     * @param key The Compose key code to test.
     * @return `true` on the first step run for the frame containing the press; `false` after.
     */
    public fun wasPressed(key: Key): Boolean = key in pressed

    /**
     * Returns whether [key] went up since the last edge-consuming step — a latched edge.
     *
     * @param key The Compose key code to test.
     * @return `true` on the first step run for the frame containing the release; `false` after.
     */
    public fun wasReleased(key: Key): Boolean = key in released
}

/* LATCH — the mailbox between the event world and the step world */

/**
 * Mutable collector bridging UI events (arriving whenever the OS delivers them) to per-step
 * [Input] snapshots (read by the semi-fixed-timestep loop). Level state tracks the latest event;
 * edge transitions are latched until [consumeEdges] reads and clears them — the first step of a
 * frame's burst calls [consumeEdges], subsequent burst steps call [level].
 *
 * Deliberately Compose-event-free (primitive `id`/[FlatPoint]/[Key] inputs) so the latching
 * semantics are unit-testable; `Game` adapts `PointerEvent`/`KeyEvent` onto these calls.
 */
internal class InputLatch {
    private class Record(
        var position: FlatPoint,
        var down: Boolean,
        var pressed: Boolean = false,
        var released: Boolean = false,
        /** Touch contact lifted — drop the record once its release edge has been consumed. */
        var ended: Boolean = false,
    )

    private val pointers = LinkedHashMap<Long, Record>()
    private val keysDown = mutableSetOf<Key>()
    private val keysPressed = mutableSetOf<Key>()
    private val keysReleased = mutableSetOf<Key>()

    /** Position update without a transition — mouse hover or drag, touch drag. */
    fun move(id: Long, position: FlatPoint) {
        val record = pointers.getOrPut(id) { Record(position, down = false) }
        record.position = position
    }

    fun press(id: Long, position: FlatPoint) {
        val record = pointers.getOrPut(id) { Record(position, down = false) }
        record.position = position
        record.down = true
        record.pressed = true
        record.ended = false
    }

    /** [ends] = the pointer ceases to exist once consumed (touch lift); a mouse persists (hover). */
    fun release(id: Long, position: FlatPoint, ends: Boolean) {
        val record = pointers.getOrPut(id) { Record(position, down = false) }
        record.position = position
        record.down = false
        record.released = true
        record.ended = ends
    }

    fun keyDown(key: Key) {
        // `add` returns false while the key is already held — that's OS auto-repeat, not an edge.
        if (keysDown.add(key)) keysPressed.add(key)
    }

    fun keyUp(key: Key) {
        keysDown.remove(key)
        keysReleased.add(key)
    }

    /** First step of a frame's burst: edges included, then cleared; ended touches dropped. */
    fun consumeEdges(): Input {
        val input = snapshot(edges = true)
        val iterator = pointers.values.iterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            if (record.ended) {
                iterator.remove()
            } else {
                record.pressed = false
                record.released = false
            }
        }
        keysPressed.clear()
        keysReleased.clear()
        return input
    }

    /** Later steps of a burst: level state only, all edge flags false. */
    fun level(): Input = snapshot(edges = false)

    private fun snapshot(edges: Boolean): Input = Input(
        pointers = pointers.values.map {
            Pointer(it.position, it.down, edges && it.pressed, edges && it.released)
        },
        keys = Keys(
            down = keysDown.toSet(),
            pressed = if (edges) keysPressed.toSet() else emptySet(),
            released = if (edges) keysReleased.toSet() else emptySet(),
        ),
    )
}
