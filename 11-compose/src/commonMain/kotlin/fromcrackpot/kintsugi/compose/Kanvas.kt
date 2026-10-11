package fromcrackpot.kintsugi.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fromcrackpot.kintsugi.compose.drawing.drawShapes
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes

/**
 * Renders the scene graph under [root] onto a transparent Compose canvas — every shape drawn in
 * order with its own style, the scene uniformly fitted to the surface.
 *
 * Layout: when no host modifier imposes a size, the canvas takes [GroupNode.bounds] as its
 * intrinsic preferred size, one bounds unit per dp, falling back to 400 × 300 dp when the root
 * carries no bounds. Host sizing in [modifier] (`Modifier.fillMaxSize()`, `Modifier.size(...)`)
 * always wins.
 *
 * Viewport: the draw pass runs at [fitScale] about the canvas center — the bounds uniformly
 * fitted to the surface, aspect preserved, letterboxed on mismatch — and is clipped to the
 * bounds rectangle, so nothing renders outside the scene and the letterbox bars stay inert;
 * a shape wholly outside bounds simply doesn't render. Without bounds there is no clip and the
 * surface is the world, one CGA unit per dp. An infinite [Line] draws as its chord through the
 * bounds-derived [worldBorder] circle, so its visible span is a property of the scene, not of
 * the surface showing it. Pointer input converts back through [canvasToCga] with the matching
 * [fitScale] value.
 *
 * Degenerate states that cannot be faithfully drawn — a [FlatPoint], [RoundPoint], or [Line] at
 * infinity, a [Line] wholly outside the world border, an imaginary [Circle] or [Dipole], an
 * inside-out [Circle] or [Polygon] — are skipped with a console warning, never an error: the
 * algebra keeps such values as first-class data; only the draw site declines them.
 *
 * The canvas paints no background — letterboxed and unpainted regions show whatever is behind
 * the composable; a scene supplies its own backdrop as content (a full-bleed rectangle [Polygon]
 * first child). Every leaf draws with its [ShapeNode.style] exactly as given — styles arrive
 * concrete from the scene graph, and no renderer-side fallback applies.
 *
 * @param root The scene to draw; its [GroupNode.bounds] is the viewport.
 * @param modifier The host's modifier chain; any size it imposes overrides the intrinsic size.
 */
@Composable
public fun Kanvas(
    root: GroupNode,
    modifier: Modifier = Modifier,
) {
    val minSize = root.bounds?.let {
        Modifier.defaultMinSize(it.width.toFloat().dp, it.height.toFloat().dp)
    } ?: Modifier.defaultMinSize(400.dp, 300.dp)

    // No remember(root) memo: per-frame-rebuilt trees make a structurally-equal-but-new root
    // common, and a memo keyed on it would pin stale leaf references.
    val shapes = root.flattenShapes()
    val density = LocalDensity.current.density

    Canvas(modifier.then(minSize)) {
        val scale = fitScale(size, root.bounds, density)
        val border = worldBorder(size, root.bounds, scale)
        scale(scale) {
            // Clip in pre-scale draw space (the space cgaToCanvas emits — the scale() transform
            // carries the rect): the bounds rectangle centered on the canvas midpoint, matching
            // the scene-graph worldBorder convention of ignoring the bounds' minX/minY offsets.
            val bounds = root.bounds
            if (bounds != null) {
                val hw = bounds.width.toFloat() / 2f
                val hh = bounds.height.toFloat() / 2f
                clipRect(
                    left = size.width / 2f - hw,
                    top = size.height / 2f - hh,
                    right = size.width / 2f + hw,
                    bottom = size.height / 2f + hh,
                ) {
                    drawShapes(shapes, border)
                }
            } else {
                drawShapes(shapes, border)
            }
        }
    }
}
