package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

/**
 * `Segment * Motor => Segment`<br>
 * Returns this segment moved by [motor]'s rigid motion.
 *
 * - The moved segment derives fresh endpoints. Shared endpoint instances do not carry over.
 *
 * @receiver The segment to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved segment.
 */
public operator fun <T : Segment> T.times(motor: Motor): T =
    narrowed(copy((this.toDipole * motor).toSegment))

/**
 * `Segment * Flector => Segment`<br>
 * Returns this segment reflected by [flector]'s mirror motion.
 *
 * - The moved segment derives fresh endpoints. Shared endpoint instances do not carry over.
 *
 * @receiver The segment to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected segment.
 */
public operator fun <T : Segment> T.times(flector: Flector): T =
    narrowed(copy((this.toDipole * flector).toSegment))

/**
 * `Segment * Versor => Segment`<br>
 * Returns this segment moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The segment to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved segment.
 */
public operator fun <T : Segment> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Segment * Dilator => Segment`<br>
 * Returns this segment dilated by [dilator].
 *
 * - The dilated segment derives fresh endpoints. Shared endpoint instances do not carry over.
 *
 * @receiver The segment to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated segment.
 */
public operator fun <T : Segment> T.times(dilator: Dilator): T =
    narrowed(copy((this.toDipole * dilator).toSegment))
