package fromcrackpot.kintsugi.scratchpad

import fromcrackpot.kintsugi.compose.tracing.canvasToTrace
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scratchpad.scenes.SMILEY_SVG
import fromcrackpot.kintsugi.svg.parseSvg
import fromcrackpot.kintsugi.svg.serializeSvg
import kotlin.test.Test

/**
 * Standing helper for `:compose`-vs-`:svg` drift investigation. The test always passes; its value
 * is the side-by-side `println` dump of both renderer outputs for the same scene. Run via
 * `./gradlew :scratchpad:desktopTest --info` (the `--info` flag preserves stdout in the gradle log)
 * whenever you suspect drift, then eyeball the two blocks.
 *
 * Add more `@Test` methods here when investigating a specific drift — one method per fixture is the
 * idiom. We deliberately don't pin assertions (no `assertEquals(canvasTrace, svgOutput)`) because
 * the two outputs are intentionally different surfaces (draw-call trace vs XML); the eyeball pass
 * is the gate.
 */
class DriftCheckTest {

    @Test
    fun `dump canvas trace and svg output for SMILEY_SVG`() {
        val group = parseSvg(SMILEY_SVG)
        val bounds = requireNotNull(group.bounds) { "parsed SVG must carry viewBox-derived bounds" }
        val shapes = group.flattenShapes()

        val canvas = canvasToTrace(shapes, bounds.width.toFloat(), bounds.height.toFloat())
        val svg = serializeSvg(group)

        println()
        println("===== CANVAS TRACE (${shapes.size} shapes flattened, ${bounds.width.toInt()}x${bounds.height.toInt()}) =====")
        println(canvas)
        println()
        println("===== SVG OUTPUT =====")
        println(svg)
        println()
    }
}
