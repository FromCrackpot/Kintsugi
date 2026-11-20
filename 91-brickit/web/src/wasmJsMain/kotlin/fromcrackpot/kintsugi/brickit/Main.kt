package fromcrackpot.kintsugi.brickit

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    embed("brickit:started", null)
    ComposeViewport(document.getElementById("game")!!) {
        App(
            onFirstFrame = { embed("brickit:ready", null) },
            onFailure = { embed("brickit:error", it.toString()) },
        )
    }
}

private fun embed(type: String, message: String?): Unit = js("window.brickitEmbed?.(type, message)")
