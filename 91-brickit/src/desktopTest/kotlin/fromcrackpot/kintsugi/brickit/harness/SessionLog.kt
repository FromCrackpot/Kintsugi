package fromcrackpot.kintsugi.brickit.harness

import fromcrackpot.kintsugi.gameengine.World
import fromcrackpot.kintsugi.svg.serializeSvg
import java.io.File

// Session artefacts: one JSONL row per step + an SVG frame every `stride` steps, under
// build/sessions/<name>/ — fresh per run.
internal class SessionLog<W : World>(
    name: String,
    private val stride: Int = 30,
    private val row: (elapsed: Double, world: W) -> String,
) {
    private val dir: File = moduleDir.resolve("build/sessions/$name").apply { deleteRecursively(); mkdirs() }
    private val log: File = dir.resolve("log.jsonl")
    private var steps = 0

    fun record(elapsed: Double, world: W) {
        log.appendText(row(elapsed, world) + "\n")
        if (steps % stride == 0) {
            dir.resolve("frame_${steps.toString().padStart(5, '0')}.svg").writeText(serializeSvg(world.root))
        }
        steps++
    }
}
