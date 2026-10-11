package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.StrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap

/**
 * Bridges the renderer-neutral [ShapeStrokeCap] to Compose's [StrokeCap] — the cap counterpart of
 * [toComposeColor]. Forward-only (unlike color, no `:compose` caller reconstructs a `ShapeStrokeCap`
 * from Compose), so it stays internal.
 */
internal fun ShapeStrokeCap.toComposeStrokeCap(): StrokeCap = when (this) {
    ShapeStrokeCap.BUTT -> StrokeCap.Butt
    ShapeStrokeCap.ROUND -> StrokeCap.Round
    ShapeStrokeCap.SQUARE -> StrokeCap.Square
}
