package fromcrackpot.kintsugi.scratchpad

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.svg.parseSvg
import fromcrackpot.kintsugi.scratchpad.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads an SVG asset from the compose-resources `files/` folder into a [GroupNode] — always a
 * group, even for a single-shape sprite: the group's `bounds` carries the asset's own viewBox,
 * which the parse-side coordinate re-mapping is relative to (the content arrives centered on the
 * CGA origin; place it in a scene with the whole-group `* T(…)`, or extract a known single-shape
 * sprite through the typed load boundary `single<N>()`).
 *
 * Suspend because resource reading is a `fetch` on wasmJs; desktop entry points can `runBlocking`.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadSvgAsset(path: String): GroupNode =
    parseSvg(Res.readBytes(path).decodeToString())
