package fromcrackpot.kintsugi.scratchpad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import fromcrackpot.kintsugi.compose.Scene
import fromcrackpot.kintsugi.gameengine.Game
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scratchpad.sketches.Boom
import fromcrackpot.kintsugi.scratchpad.sketches.SMILEY_SVG
import fromcrackpot.kintsugi.scratchpad.sketches.Sfx
import fromcrackpot.kintsugi.scratchpad.sketches.brick
import fromcrackpot.kintsugi.scratchpad.sketches.emptyScene
import fromcrackpot.kintsugi.scratchpad.sketches.logoScene
import fromcrackpot.kintsugi.svg.parseSvg

/**
 * A named scratchpad sketch. Each sketch carries the two things the scratchpad needs of it:
 *  - [snapshot]: a static [GroupNode] used for SVG export on desktop (see desktopMain `main`).
 *    Suspend so a sketch may load SVG assets via [loadSvgAsset] (resource reading is a `fetch` on
 *    wasmJs); desktop entry points `runBlocking` it.
 *  - [content]: how the sketch renders on screen. Static sketches use the default (a [Scene] of
 *    [snapshot], produced asynchronously, so nothing renders until the group arrives); interactive
 *    sketches override it with their own stateful composable (see `:brickit` for a full game).
 *
 * Both capabilities live on one type so the registry ([sketches]) is the single source of truth:
 * adding a sketch is one list entry, with no entry-point or [App] changes.
 */
class Sketch(
    val name: String,
    val snapshot: suspend () -> GroupNode,
    val content: @Composable (Modifier) -> Unit = { modifier ->
        val root by produceState<GroupNode?>(initialValue = null, name) { value = snapshot() }
        root?.let { Scene(it, modifier) }
    },
)

/** Sketch shown when no name is supplied (e.g. the browser run, or `:scratchpad:run` with no `-Psketch=`). */
const val DEFAULT_SKETCH = "smiley"

/**
 * The sketch registry. An SVG-authored sketch (parsed via [parseSvg]) and a programmatically-built
 * one coexist behind one uniform lookup; each entry lazily builds its [GroupNode] snapshot.
 */
val sketches: List<Sketch> = listOf(
    Sketch("empty", snapshot = { emptyScene() }),
    Sketch("smiley", snapshot = { parseSvg(SMILEY_SVG) }),
    Sketch("logo", snapshot = { logoScene() }),
    Sketch("boom", snapshot = { Boom(brick()).root }, content = { modifier -> Game(modifier) { Boom(brick()) } }),
    Sketch("sfx", snapshot = { emptyScene() }, content = { modifier -> Sfx(modifier) }),
)

/** Resolve a sketch name (trimmed, case-insensitive) to its [Sketch], falling back to [DEFAULT_SKETCH]. */
fun sketchFor(name: String?): Sketch {
    val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: DEFAULT_SKETCH
    return sketches.firstOrNull { it.name == key }
        ?: sketches.first { it.name == DEFAULT_SKETCH }.also {
            println("scratchpad: unknown sketch '$key'; known: ${sketches.joinToString { s -> s.name }}. Using '$DEFAULT_SKETCH'.")
        }
}
