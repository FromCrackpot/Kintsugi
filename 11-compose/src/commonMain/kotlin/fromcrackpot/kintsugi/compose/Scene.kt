package fromcrackpot.kintsugi.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fromcrackpot.kintsugi.compose.drawing.drawShapes
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scenegraph.style.ImaginaryChrome

/**
 * Renders the scene graph under [root] onto a transparent Compose canvas. Every shape draws in
 * order with its own style, and the scene is uniformly fitted to the surface.
 *
 * - Each child of [root] is its own cached layer, redrawn only when another instance takes its place.
 * - Sized by the root's bounds unless the host sizes it. The scene is fitted, letterboxed and clipped.
 * - Imaginary shapes draw dashed, and their point markers as rings, per [ImaginaryChrome].
 * - Skipped with a warning: points, lines, dipoles and segments at infinity; inside-out and zero-length shapes.
 *
 * @param root The scene to draw. Its [GroupNode.bounds] is the viewport, and a backdrop goes first.
 * @param modifier The host's modifier chain. Any size it imposes overrides the intrinsic size.
 */
@Composable
public fun Scene(
    root: GroupNode,
    modifier: Modifier = Modifier,
) {
    val minSize = root.bounds?.let {
        Modifier.defaultMinSize(it.width.toFloat().dp, it.height.toFloat().dp)
    } ?: Modifier.defaultMinSize(400.dp, 300.dp)
    val density = LocalDensity.current.density

    Box(modifier.then(minSize)) {
        for (child in root.children) SceneLayer(child, root.bounds, density)
    }
}

// Skipped while node == last frame's, replaying its recorded layer. The compiler treats Node as
// stable (rounded structural equals, identity fast path); sound only because nodes are immutable.
@Composable
private fun BoxScope.SceneLayer(node: Node, bounds: SceneBounds?, density: Float) {
    Spacer(
        Modifier
            .matchParentSize()
            .graphicsLayer()
            .drawWithCache {
                val shapes = when (node) {
                    is ShapeNode -> listOf(node)
                    is GroupNode -> node.flattenShapes()
                }
                val scale = fitScale(size, bounds, density)
                val border = worldBorder(size, bounds, scale)
                onDrawBehind { drawFitted(bounds, scale) { drawShapes(shapes, border) } }
            },
    )
}

private inline fun DrawScope.drawFitted(
    bounds: SceneBounds?,
    scale: Float,
    crossinline block: DrawScope.() -> Unit,
) {
    scale(scale) {
        // Clip in pre-scale draw space, the space cgaToCanvas emits; the scale() transform carries
        // the rect. The clip is the bounds rectangle centered on the canvas midpoint, ignoring the
        // bounds' minX/minY like the scene-graph worldBorder convention.
        if (bounds != null) {
            val hw = bounds.width.toFloat() / 2f
            val hh = bounds.height.toFloat() / 2f
            clipRect(
                left = size.width / 2f - hw,
                top = size.height / 2f - hh,
                right = size.width / 2f + hw,
                bottom = size.height / 2f + hh,
            ) {
                block()
            }
        } else {
            block()
        }
    }
}
