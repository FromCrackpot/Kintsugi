package fromcrackpot.kintsugi.gameengine.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerType
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint

/* SNAPSHOTS: what the `state` lambda sees, once per step */

/**
 * One step's input snapshot of every active pointer and the keyboard. It holds sampled state,
 * never events.
 *
 * - Edges are latched per frame: its first step reads them, later steps see level state only.
 * - Several presses within one frame read as one edge.
 * - A mouse persists while up (hover). Any other pointer drops once its release edge is consumed.
 */
public class Input internal constructor(
    /** The active pointers in press order: a mouse always, others until their release is consumed. */
    public val pointers: List<Pointer>,
    /** The keyboard snapshot. */
    public val keys: Keys,
) {
    /** The first active pointer, or `null` when there is none. A single-pointer game reads this one. */
    public val pointer: Pointer? get() = pointers.firstOrNull()

    public companion object {
        /** The neutral snapshot: no pointers, no keys, no edges. It is the input of a step driven without a user. */
        public val NONE: Input = Input(emptyList(), Keys(emptySet(), emptySet(), emptySet()))

        /**
         * Builds an input snapshot from hand-fabricated state, with keys and pointers stated explicitly
         * instead of sampled.
         *
         * - Nothing checks an edge against the level state: the caller owns that contract.
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
 * One pointer, such as a mouse, a touch contact or a stylus, in CGA model units.
 *
 * - Its flags are independent: a click inside one frame reads not down, yet pressed and released.
 */
public class Pointer internal constructor(
    /** Where the pointer is, as a [FlatPoint] in CGA model units. */
    public val position: FlatPoint,
    /** Whether the pointer is down. `false` for a hovering mouse. */
    public val isDown: Boolean,
    /** Whether a press landed since the last edge-consuming step, as a latched edge. */
    public val wasPressed: Boolean,
    /** Whether a release landed since the last edge-consuming step, as a latched edge. */
    public val wasReleased: Boolean,
    /** The pointer's device: `PointerType.Mouse`, `PointerType.Touch`, `PointerType.Stylus`, … */
    public val type: PointerType,
) {
    public companion object {
        /**
         * Builds a pointer snapshot from hand-fabricated state, the explicit counterpart of a sampled pointer.
         *
         * - Nothing checks an edge against the level state: the caller owns that contract.
         *
         * @param position Where the pointer is, in CGA model units.
         * @param isDown Whether the pointer is pressed.
         * @param wasPressed Whether a press edge is claimed.
         * @param wasReleased Whether a release edge is claimed.
         * @param type The pointer's device, a mouse by default.
         * @return The pointer snapshot, indistinguishable from a sampled one.
         */
        public fun of(
            position: FlatPoint,
            isDown: Boolean = false,
            wasPressed: Boolean = false,
            wasReleased: Boolean = false,
            type: PointerType = PointerType.Mouse,
        ): Pointer = Pointer(position, isDown, wasPressed, wasReleased, type)
    }
}

/**
 * The keyboard snapshot, over Compose `Key` codes.
 *
 * - OS auto-repeat is filtered: holding a key yields one press edge, not a stream.
 */
public class Keys internal constructor(
    private val down: Set<Key>,
    internal val pressed: Set<Key>,
    private val released: Set<Key>,
) {
    /**
     * Returns whether [key] is held down, as level state.
     *
     * @param key The Compose key code to test.
     * @return `true` while the key is held.
     */
    public fun isDown(key: Key): Boolean = key in down

    /**
     * Returns whether [key] went down since the last edge-consuming step, as a latched edge.
     *
     * @param key The Compose key code to test.
     * @return `true` on the first step run for the frame containing the press, `false` after.
     */
    public fun wasPressed(key: Key): Boolean = key in pressed

    /**
     * Returns whether [key] went up since the last edge-consuming step, as a latched edge.
     *
     * @param key The Compose key code to test.
     * @return `true` on the first step run for the frame containing the release, `false` after.
     */
    public fun wasReleased(key: Key): Boolean = key in released
}

/* LATCH: the mailbox between the event world and the step world */

/**
 * Mutable collector bridging UI events (arriving whenever the OS delivers them) to per-step
 * [Input] snapshots (read by the semi-fixed-timestep loop).
 *
 * - Level state tracks the latest event. Edge transitions are latched until [consumeEdges] reads
 *   and clears them: the first step of a frame's burst calls [consumeEdges], later steps [level].
 * - Deliberately Compose-event-free (`id`/[FlatPoint]/[PointerType]/[Key] inputs) so the latching
 *   semantics are unit-testable. `Game` adapts `PointerEvent`/`KeyEvent` onto these calls.
 * - A mouse persists after release (hover). Any other pointer ends once its release is consumed.
 */
internal class InputLatch {
    private class Record(
        val type: PointerType,
        var position: FlatPoint,
        var down: Boolean,
        var pressed: Boolean = false,
        var released: Boolean = false,
        /** Non-mouse pointer lifted: drop the record once its release edge has been consumed. */
        var ended: Boolean = false,
    )

    private val pointers = LinkedHashMap<Long, Record>()
    private val keysDown = mutableSetOf<Key>()
    private val keysPressed = mutableSetOf<Key>()
    private val keysReleased = mutableSetOf<Key>()

    /** Position update without a transition: mouse hover or drag, touch drag. */
    fun move(id: Long, position: FlatPoint, type: PointerType) {
        val record = pointers.getOrPut(id) { Record(type, position, down = false) }
        record.position = position
    }

    fun press(id: Long, position: FlatPoint, type: PointerType) {
        val record = pointers.getOrPut(id) { Record(type, position, down = false) }
        record.position = position
        record.down = true
        record.pressed = true
        record.ended = false
    }

    fun release(id: Long, position: FlatPoint, type: PointerType) {
        val record = pointers.getOrPut(id) { Record(type, position, down = false) }
        record.position = position
        record.down = false
        record.released = true
        record.ended = record.type != PointerType.Mouse
    }

    fun keyDown(key: Key) {
        // `add` returns false while the key is already held: that's OS auto-repeat, not an edge.
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
            Pointer(it.position, it.down, edges && it.pressed, edges && it.released, it.type)
        },
        keys = Keys(
            down = keysDown.toSet(),
            pressed = if (edges) keysPressed.toSet() else emptySet(),
            released = if (edges) keysReleased.toSet() else emptySet(),
        ),
    )
}
