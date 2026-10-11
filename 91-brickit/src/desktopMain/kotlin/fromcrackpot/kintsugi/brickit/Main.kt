package fromcrackpot.kintsugi.brickit

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    application {
        // Outer size — the client area is a bit smaller (decorations), and Kanvas's fit scaling
        // fits the arena into whatever it turns out to be. No exact-client contortions needed.
        Window(
            onCloseRequest = ::exitApplication,
            title = "Brick'It",
            state = rememberWindowState(size = DpSize(800.dp, 600.dp)),
            resizable = true,
        ) {
            App()
        }
    }
}
