package fromcrackpot.kintsugi.scratchpad.clips

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import fromcrackpot.kintsugi.compose.Scene
import fromcrackpot.kintsugi.gameengine.Controls
import fromcrackpot.kintsugi.gameengine.drive
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scratchpad.sketches.Boom
import fromcrackpot.kintsugi.scratchpad.sketches.brick
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/* TUNING */

private const val FPS = 50
private const val HOLD = 0.5
// SIZE / CROP = 2.5 px per unit: tile edges land on whole pixels. A fractional pitch AA-seams the flush brick
private const val SIZE = 800
private val CROP = SceneBounds(-160.0, -160.0, 320.0, 320.0)

// IDE runs may start in the repo root or the module dir, so probe both.
private val moduleDir: File = File(System.getProperty("user.dir")).let { cwd ->
    if (cwd.resolve("src/desktopMain").isDirectory) cwd else cwd.resolve("90-scratchpad")
}

// One blast of the boom sketch as a seamless looping GIF: build/clips/boom/{frames/, boom.gif}.
fun main() {
    val dir = moduleDir.resolve("build/clips/boom").apply { deleteRecursively() }
    val frames = dir.resolve("frames").apply { mkdirs() }

    var root by mutableStateOf(GroupNode())
    val scene = ImageComposeScene(SIZE, SIZE, Density(1f)) { Scene(root, Modifier.fillMaxSize()) }
    var count = 0
    var looped = false
    try {
        drive(
            world = { Boom(brick()) },
            seconds = HOLD + 3.0,
            tickRate = FPS,
            onStep = { _, w ->
                looped = looped || (w.running && w.elapsed == 0.0)
                if (looped) return@drive
                root = GroupNode(w.root.children, CROP)
                Snapshot.sendApplyNotifications()
                val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
                frames.resolve("f%04d.png".format(count++)).writeBytes(png)
            },
            controls = { t, _ -> if (t >= HOLD) Controls(setOf(Key.Spacebar)) else Controls.NONE },
        )
    } finally {
        scene.close()
    }
    check(looped) { "the blast never culled down to a fresh brick" }

    val gif = dir.resolve("boom.gif")
    val ffmpeg = ProcessBuilder(
        "ffmpeg", "-y", "-loglevel", "error",
        "-framerate", "$FPS", "-i", frames.resolve("f%04d.png").path,
        "-vf", "split[a][b];[a]palettegen=reserve_transparent=0[p];[b][p]paletteuse=dither=none",
        "-loop", "0", gif.path,
    ).redirectErrorStream(true).start()
    val log = ffmpeg.inputStream.bufferedReader().readText()
    check(ffmpeg.waitFor() == 0) { log }
    println("boom clip: wrote ${gif.absolutePath}")
}
