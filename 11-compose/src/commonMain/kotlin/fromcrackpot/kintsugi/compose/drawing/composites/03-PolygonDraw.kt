package fromcrackpot.kintsugi.compose.drawing.composites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon

internal fun DrawScope.drawPolygon(polygon: Polygon, fill: Color, stroke: Color, strokeWidth: Float) {
    if (polygon.isInsideOut) {
        warn("skipping inside-out Polygon (signedArea < 0): $polygon")
        return
    }
    // No other degenerate guard: the Polygon ctor makes < 3 vertices, at-infinity vertices, and
    // collapsed edges unrepresentable — same by-construction posture as drawSegment.
    val path = Path().apply {
        polygon.vertices.forEachIndexed { i, vertex ->
            val p = cgaToCanvas(vertex.px, vertex.py)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    // Compose's drawPath takes a single DrawStyle (Fill OR Stroke), never both — so both
    // layers are two draw calls, fill first, stroke on top.
    drawPath(path, color = fill)
    drawPath(path, color = stroke, style = Stroke(width = strokeWidth))
}
