package fromcrackpot.kintsugi.scratchpad

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Scene selection is desktop-only (Gradle `-Pscene=`); the browser always shows the default.
    ComposeViewport {
        App()
    }
}
