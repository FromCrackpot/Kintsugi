package fromcrackpot.kintsugi.scratchpad

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import fromcrackpot.kintsugi.svg.writeTo
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
    val sceneName = args.firstOrNull()
    val scene = sceneFor(sceneName)
    writeSceneSvg(scene)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kintsugi Scratchpad",
        ) {
            App(sceneName)
        }
    }
}

/**
 * Round-trip demo: snapshot the selected scene's GroupNode, then serialize and write it back out.
 * Confirms the full :svg pipeline (sceneFor + snapshot + writeTo) works end-to-end on desktop
 * for whichever scene was selected.
 */
private fun writeSceneSvg(scene: Scene) {
    val outFile = File("build/scratchpad-out/${scene.name}.svg")
    outFile.parentFile?.mkdirs()
    // snapshot is suspend (wasmJs resource loading); at a JVM entry point runBlocking is idiomatic.
    runBlocking { scene.snapshot() }.writeTo(outFile)
    println("scratchpad: wrote ${outFile.absolutePath}")
}
