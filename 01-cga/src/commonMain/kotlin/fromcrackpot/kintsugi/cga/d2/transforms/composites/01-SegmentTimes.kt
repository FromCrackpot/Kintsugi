package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

/**
 * `Segment * Motor => Segment`<br>
 * Returns this segment moved by [motor]'s rigid motion.
 *
 * Moves the wrapped blade — `Dipole * Motor` carries the geometric contract — and wraps the
 * result: the segment's endpoints move together, [Segment.start] staying `start`. The moved
 * segment derives its endpoints from the moved blade; the endpoint instances a
 * constructor-built segment shares by reference are not carried across the move. The result
 * is the receiver's own class: the moved geometry returns through [Segment.copy], preserving
 * a subclass that overrides the hook.
 *
 * @receiver The [Segment] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved segment, as the receiver's class.
 */
public operator fun <T : Segment> T.times(motor: Motor): T =
    narrowed(copy((this.toDipole * motor).toSegment))

/**
 * `Segment * Flector => Segment`<br>
 * Returns this segment reflected by [flector]'s mirror motion.
 *
 * Reflects the wrapped blade — `Dipole * Flector` carries the geometric contract — and wraps
 * the result: the segment's endpoints reflect together, [Segment.start] staying `start`. The
 * reflected segment derives its endpoints from the moved blade; the endpoint instances a
 * constructor-built segment shares by reference are not carried across the move. The result
 * is the receiver's own class: the moved geometry returns through [Segment.copy], preserving
 * a subclass that overrides the hook.
 *
 * @receiver The [Segment] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected segment, as the receiver's class.
 */
public operator fun <T : Segment> T.times(flector: Flector): T =
    narrowed(copy((this.toDipole * flector).toSegment))

/**
 * `Segment * Versor => Segment`<br>
 * Returns this segment moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Segment * Motor` and
 * `Segment * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Segment] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved segment, as the receiver's class.
 */
public operator fun <T : Segment> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
