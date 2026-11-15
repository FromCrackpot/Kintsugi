package fromcrackpot.kintsugi.gameengine.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerType
import fromcrackpot.kintsugi.gameengine.Step

/**
 * Horizontal motion commanded by the player: a mouse's x is followed absolutely, a finger drags
 * relatively like on a trackpad, and the key pair slides while neither moves.
 *
 * - One instance per followed entity: it latches the last pointer and finger samples across steps.
 * - The newest finger down drags. A finger landing or lifting re-grips without a jump.
 * - The motion is unconstrained. Clamping it to a region is the caller's business.
 *
 * @constructor Builds the follower with its tuning. The first mouse sample counts as moved.
 * @param speed The keyboard slide speed, in model units per second.
 * @param left The Compose key code sliding toward negative x while held.
 * @param right The Compose key code sliding toward positive x while held.
 * @param dragGain The model units moved per model unit a finger drags, 1 by default.
 */
public class FollowInputX(
    private val speed: Double,
    private val left: Key,
    private val right: Key,
    private val dragGain: Double = 1.0,
) {
    private var lastPointerX: Double? = null
    private var lastFingerX: Double? = null

    /**
     * Returns the step's horizontal displacement for an entity at [currentX], advancing the pointer
     * and finger latches.
     *
     * - Reads the current [Step]'s input. Call it once per step, where the followed entity moves.
     *
     * @param currentX The followed entity's current x-coordinate.
     * @return The displacement to apply this step, in model units.
     */
    context(step: Step)
    public fun dx(currentX: Double): Double {
        val pointers = step.input.pointers
        val pointerX = pointers.firstOrNull { it.type != PointerType.Touch }?.position?.px
        val fingerX = pointers.lastOrNull { it.type == PointerType.Touch && it.isDown }?.position?.px
        val regrip = pointers.any { it.type == PointerType.Touch && (it.wasPressed || it.wasReleased) }
        val lastFinger = lastFingerX
        val drag = if (fingerX != null && lastFinger != null && !regrip) (fingerX - lastFinger) * dragGain else 0.0
        val displacement = when {
            pointerX != null && pointerX != lastPointerX -> pointerX - currentX
            drag != 0.0 -> drag
            else -> axis(step.input.keys) * speed * step.dt
        }
        lastPointerX = pointerX
        lastFingerX = fingerX
        return displacement
    }

    private fun axis(keys: Keys): Double =
        (if (keys.isDown(right)) 1.0 else 0.0) - (if (keys.isDown(left)) 1.0 else 0.0)
}
