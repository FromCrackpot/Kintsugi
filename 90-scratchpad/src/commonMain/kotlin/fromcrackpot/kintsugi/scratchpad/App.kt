package fromcrackpot.kintsugi.scratchpad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun App(sketchName: String? = null) {
    val sketch = remember(sketchName) { sketchFor(sketchName) }
    sketch.content(Modifier.fillMaxSize())
}
