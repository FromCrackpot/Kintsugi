package fromcrackpot.kintsugi.scratchpad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun App(sceneName: String? = null) {
    val scene = remember(sceneName) { sceneFor(sceneName) }
    scene.content(Modifier.fillMaxSize())
}
