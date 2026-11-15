package fromcrackpot.kintsugi.scratchpad

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import fromcrackpot.kintsugi.svg.writeTo
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
    val sketchName = args.firstOrNull()
    val sketch = sketchFor(sketchName)
    writeSketchSvg(sketch)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kintsugi Scratchpad",
        ) {
            App(sketchName)
        }
    }
}

/**
 * Round-trip demo: snapshot the selected sketch's GroupNode, then serialize and write it back out.
 * Confirms the full :svg pipeline (sketchFor + snapshot + writeTo) works end-to-end on desktop
 * for whichever sketch was selected.
 */
private fun writeSketchSvg(sketch: Sketch) {
    val outFile = File("build/scratchpad-out/${sketch.name}.svg")
    outFile.parentFile?.mkdirs()
    // snapshot is suspend (wasmJs resource loading); at a JVM entry point runBlocking is idiomatic.
    runBlocking { sketch.snapshot() }.writeTo(outFile)
    println("scratchpad: wrote ${outFile.absolutePath}")
}
