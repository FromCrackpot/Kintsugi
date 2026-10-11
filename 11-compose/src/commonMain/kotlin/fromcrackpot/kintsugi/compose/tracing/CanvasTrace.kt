package fromcrackpot.kintsugi.compose.tracing

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import fromcrackpot.kintsugi.compose.Kanvas
import fromcrackpot.kintsugi.compose.drawing.drawShapes
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.worldBorder

/**
 * Returns the draw calls [nodes] produce as a normalized multi-line text trace — geometry and
 * style assertions without pixel rasterization.
 *
 * Each leaf runs through the production draw pass — the same dispatch [Kanvas] draws with —
 * against a [width] × [height] canvas at density 1 with no declared bounds: the surface is the
 * world, and infinite lines clip against the circle circumscribing it. Each line formats one
 * Compose canvas call (`rect`, `line`, `circle`, `path`) with its resolved geometry and paint;
 * a leaf the draw pass skips produces no line, and `path` lines carry the path's bounding box —
 * vertex data is not part of the trace. Coordinates round to three decimals, absorbing
 * `Double` → `Float` jitter; colors print as `#rrggbb`, alpha-prefixed as `#aarrggbb` when not
 * opaque.
 *
 * @param nodes The styled leaves to trace, drawn in order.
 * @param width The canvas width in px.
 * @param height The canvas height in px.
 * @return One line per recorded draw call, newline-joined.
 */
public fun canvasToTrace(nodes: List<ShapeNode>, width: Float, height: Float): String {
    val canvas = RecordingCanvas()
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(width, height),
    ) {
        // Border from the trace canvas size (scale-blind, density 1) — the null-bounds viewport
        // case of `worldBorder` in CoordinateMapping.kt; keeps goldens insensitive to fit scaling.
        drawShapes(nodes, worldBorder(width.toDouble(), height.toDouble()))
    }
    return canvas.events.joinToString("\n") { it.format() }
}
