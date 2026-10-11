package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key

/**
 * Horizontal motion commanded by the player: the pointer's x is followed absolutely, and the
 * key pair slides while the pointer rests.
 *
 * A world-owned helper serving one followed entity — the last-input-wins arbitration needs one
 * latched sample across steps, so each follower gets its own instance. Arbitration is decided
 * per call: the pointer commands only when its sampled x differs from the previous call's
 * sample; otherwise the motion is `axis * speed * dt` over the step's duration, where the axis
 * is `+1` with only `right` held, `-1` with only `left`, and `0` with both or neither — and a
 * call with no pointer at all takes the key path too. The comparison is bit-exact by design —
 * a resting pointer repeats the identical sampled `Double`, so no tolerance is involved. The
 * motion is unconstrained; clamping it to a region is the caller's business.
 *
 * @constructor Builds the follower with its tuning; the latch starts empty, so the first
 *   pointer sample counts as moved.
 * @param speed The keyboard slide speed, in model units per second.
 * @param left The Compose key code sliding toward negative x while held.
 * @param right The Compose key code sliding toward positive x while held.
 */
public class FollowInputX(
    private val speed: Double,
    private val left: Key,
    private val right: Key,
) {
    private var lastPointerX: Double? = null

    /**
     * Returns the step's horizontal displacement for an entity at [currentX], advancing the
     * pointer latch.
     *
     * Reads the current [Step]'s input through the arbitration above and latches the pointer
     * sample — call once per step, from the one place that moves the followed entity.
     *
     * @param currentX The followed entity's current x-coordinate.
     * @return The displacement to apply this step, in model units.
     */
    context(step: Step)
    public fun dx(currentX: Double): Double {
        val pointerX = step.input.pointer?.position?.px
        val targetX =
            if (pointerX != null && pointerX != lastPointerX) pointerX
            else currentX + axis(step.input.keys) * speed * step.dt
        lastPointerX = pointerX
        return targetX - currentX
    }

    private fun axis(keys: Keys): Double =
        (if (keys.isDown(right)) 1.0 else 0.0) - (if (keys.isDown(left)) 1.0 else 0.0)
}
