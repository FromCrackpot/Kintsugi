package fromcrackpot.kintsugi.scratchpad

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val sketch = sketchParam()
    ComposeViewport {
        App(sketch)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun sketchParam(): String? = js("new URLSearchParams(window.location.search).get('sketch')")
