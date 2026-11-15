package fromcrackpot.kintsugi.compose.tracing

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import fromcrackpot.kintsugi.compose.Scene
import fromcrackpot.kintsugi.compose.drawing.drawShapes
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.worldBorder

/**
 * Returns the draw calls [nodes] produce as a normalized multi-line text trace. It supports
 * geometry and style assertions without pixel rasterization.
 *
 * - Runs the same draw pass as [Scene], at density 1 with no declared bounds.
 * - One line per Compose canvas call. Skipped leaves produce none, and `path` lines carry a bounding box.
 * - Coordinates round to three decimals. Colors print as `#rrggbb`, or `#aarrggbb` when not opaque.
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
        // Border from the trace canvas size (scale-blind, density 1): the null-bounds viewport
        // case of `worldBorder` in CoordinateMapping.kt. Keeps goldens insensitive to fit scaling.
        drawShapes(nodes, worldBorder(width.toDouble(), height.toDouble()))
    }
    return canvas.events.joinToString("\n") { it.format() }
}
