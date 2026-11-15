package fromcrackpot.kintsugi.compose.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.cgaToCanvas
import fromcrackpot.kintsugi.compose.drawing.composites.drawPolygon
import fromcrackpot.kintsugi.compose.drawing.composites.drawPolyline
import fromcrackpot.kintsugi.compose.drawing.composites.drawSegment
import fromcrackpot.kintsugi.compose.drawing.primitives.drawCircle
import fromcrackpot.kintsugi.compose.drawing.primitives.drawDipole
import fromcrackpot.kintsugi.compose.drawing.primitives.drawFlatPoint
import fromcrackpot.kintsugi.compose.drawing.primitives.drawLine
import fromcrackpot.kintsugi.compose.drawing.primitives.drawRoundPoint
import fromcrackpot.kintsugi.compose.style.toComposeColor
import fromcrackpot.kintsugi.compose.style.toComposeStrokeCap
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.CompositeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.PrimitiveNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode

internal fun DrawScope.cgaToCanvas(cgaX: Double, cgaY: Double): Offset =
    cgaToCanvas(cgaX, cgaY, size)

/**
 * The one draw pass. It dispatches every shape through [drawShape] against the caller's
 * [worldBorder], built once per pass from the bounds (see `worldBorder` in `CoordinateMapping.kt`).
 *
 * - `Scene` and `canvasToTrace` both route through here, so the trace tests exercise exactly what
 *   the composable draws. The two can't drift.
 */
internal fun DrawScope.drawShapes(nodes: List<ShapeNode>, worldBorder: Circle) {
    nodes.forEach { drawShape(it, worldBorder) }
}

// Every paint layer draws unconditionally, transparent included. Alpha-0 culling is Skia's job
// (`nothingToDraw()`), not ours. One policy for all eight shapes, so traces show `#00000000`
// entries for unset layers by design.
internal fun DrawScope.drawShape(node: ShapeNode, worldBorder: Circle) {
    val style = node.style
    val fill = style.fill.toComposeColor()
    val stroke = style.stroke.toComposeColor()
    val cap = style.strokeCap.toComposeStrokeCap()

    // ShapeNode is unsealed but its two families are sealed. The nested `when`s stay exhaustive, so
    // a new leaf is a compile error here, and the `else` guards only a foreign ShapeNode family.
    when (node) {
        is PrimitiveNode -> when (node) {
            is FlatPointNode -> drawFlatPoint(node, fill, style.size.toFloat())
            is LineNode -> drawLine(node, worldBorder, stroke, style.strokeWidth.toFloat(), cap)
            is RoundPointNode -> drawRoundPoint(node, fill, style.size.toFloat())
            is DipoleNode -> drawDipole(node, fill, style.size.toFloat())
            is CircleNode -> drawCircle(node, fill, stroke, style.strokeWidth.toFloat())
        }
        is CompositeNode -> when (node) {
            is SegmentNode -> drawSegment(node, stroke, style.strokeWidth.toFloat(), cap)
            is PolylineNode -> drawPolyline(node, fill, stroke, style.strokeWidth.toFloat(), cap)
            is PolygonNode -> drawPolygon(node, fill, stroke, style.strokeWidth.toFloat())
        }
        else -> error("unhandled ShapeNode family: $node")
    }
}
