package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.PathEffect
import fromcrackpot.kintsugi.scenegraph.style.ShapeDash

/**
 * Bridges the renderer-neutral [ShapeDash] to a Compose dash [PathEffect], `null` when solid.
 * Lengths stay in model units. The draw pass's fit `scale()` scales them with the geometry.
 */
internal fun ShapeDash.toPathEffect(): PathEffect? =
    if (isSolid) null
    else PathEffect.dashPathEffect(FloatArray(intervals.size) { intervals[it].toFloat() }, offset.toFloat())
