package fromcrackpot.kintsugi.scratchpad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import fromcrackpot.kintsugi.compose.Kanvas
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scratchpad.scenes.SMILEY_SVG
import fromcrackpot.kintsugi.scratchpad.scenes.emptyScene
import fromcrackpot.kintsugi.scratchpad.scenes.logoScene
import fromcrackpot.kintsugi.svg.parseSvg

/**
 * A named scratchpad scene. Each scene carries the two things the scratchpad needs of it:
 *  - [snapshot]: a static [GroupNode] used for SVG export on desktop (see desktopMain `main`).
 *    Suspend so a scene may load SVG assets via [loadSvgAsset] (resource reading is a `fetch` on
 *    wasmJs); desktop entry points `runBlocking` it.
 *  - [content]: how the scene renders on screen. Static scenes use the default (a [Kanvas] of
 *    [snapshot], produced asynchronously — nothing renders until the group arrives); interactive
 *    scenes override it with their own stateful composable (see `:brickit` for a full game).
 *
 * Both capabilities live on one type so the registry ([scenes]) is the single source of truth —
 * adding a scene is one list entry, with no entry-point or [App] changes.
 */
class Scene(
    val name: String,
    val snapshot: suspend () -> GroupNode,
    val content: @Composable (Modifier) -> Unit = { modifier ->
        val root by produceState<GroupNode?>(initialValue = null, name) { value = snapshot() }
        root?.let { Kanvas(it, modifier) }
    },
)

/** Scene shown when no name is supplied (e.g. the browser run, or `:scratchpad:run` with no `-Pscene=`). */
const val DEFAULT_SCENE = "smiley"

/**
 * The scene registry. An SVG-authored scene (parsed via [parseSvg]) and a programmatically-built
 * one coexist behind one uniform lookup; each entry lazily builds its [GroupNode] snapshot.
 */
val scenes: List<Scene> = listOf(
    Scene("empty", snapshot = { emptyScene() }),
    Scene("smiley", snapshot = { parseSvg(SMILEY_SVG) }),
    Scene("logo", snapshot = { logoScene() }),
)

/** Resolve a scene name (trimmed, case-insensitive) to its [Scene], falling back to [DEFAULT_SCENE]. */
fun sceneFor(name: String?): Scene {
    val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: DEFAULT_SCENE
    return scenes.firstOrNull { it.name == key }
        ?: scenes.first { it.name == DEFAULT_SCENE }.also {
            println("scratchpad: unknown scene '$key'; known: ${scenes.joinToString { s -> s.name }}. Using '$DEFAULT_SCENE'.")
        }
}
