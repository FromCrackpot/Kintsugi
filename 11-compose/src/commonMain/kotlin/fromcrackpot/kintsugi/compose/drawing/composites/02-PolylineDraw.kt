package fromcrackpot.kintsugi.compose.drawing.composites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline

internal fun DrawScope.drawPolyline(
    polyline: Polyline,
    fill: Color,
    stroke: Color,
    strokeWidth: Float,
    cap: StrokeCap,
) {
    // No degenerate guard: the Polyline ctor makes < 2 vertices, at-infinity vertices, and
    // collapsed edges unrepresentable — same by-construction posture as drawSegment.
    // Same Path walk as drawPolygon but WITHOUT close(): the chain stays open, so the stroke
    // has ends and honors the cap (segment's rule). Note Compose fills an open path as if
    // closed (matching SVG); the default polyline style is stroke-only, so fill only shows
    // when a caller asks for it explicitly.
    val path = Path().apply {
        polyline.vertices.forEachIndexed { i, vertex ->
            val p = cgaToCanvas(vertex.px, vertex.py)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
    }
    drawPath(path, color = fill)
    drawPath(path, color = stroke, style = Stroke(width = strokeWidth, cap = cap))
}
