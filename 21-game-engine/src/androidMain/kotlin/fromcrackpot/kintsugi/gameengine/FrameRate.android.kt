package fromcrackpot.kintsugi.gameengine

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun PreferFrameRate(rate: Double?) {
    val view = LocalView.current
    DisposableEffect(view, rate) {
        val window = view.context.activity()?.window
        val display = view.display
        if (rate == null || window == null || display == null) return@DisposableEffect onDispose {}
        val current = display.mode
        val modes = display.supportedModes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }
        val chosen = modes[pickRate(modes.map { it.refreshRate.toDouble() }, rate)]
        val previous = window.attributes.preferredDisplayModeId
        window.attributes = window.attributes.apply { preferredDisplayModeId = chosen.modeId }
        onDispose { window.attributes = window.attributes.apply { preferredDisplayModeId = previous } }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
