package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.StrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap

/**
 * Bridges the renderer-neutral [ShapeStrokeCap] to Compose's [StrokeCap], the cap counterpart of
 * [toComposeColor]. Forward-only and internal, since no `:compose` caller converts a Compose cap back.
 */
internal fun ShapeStrokeCap.toComposeStrokeCap(): StrokeCap = when (this) {
    ShapeStrokeCap.BUTT -> StrokeCap.Butt
    ShapeStrokeCap.ROUND -> StrokeCap.Round
    ShapeStrokeCap.SQUARE -> StrokeCap.Square
}
