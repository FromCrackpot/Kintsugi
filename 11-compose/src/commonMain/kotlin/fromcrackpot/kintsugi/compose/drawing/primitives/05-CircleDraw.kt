package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle

internal fun DrawScope.drawCircle(
    circle: Circle,
    fill: Color,
    stroke: Color,
    strokeWidth: Float,
) {
    if (circle.isImaginary) {
        warn("skipping imaginary Circle (radius² < 0): $circle")
        return
    }
    if (circle.cw > 0) {
        warn("skipping inside-out Circle (cw > 0): $circle")
        return
    }
    val center = cgaToCanvas(circle.center.ax, circle.center.ay)
    val radiusPx = circle.radius.toFloat()
    // Compose's drawCircle takes a single DrawStyle (Fill OR Stroke), never both — so both
    // layers are two draw calls, fill first, stroke on top.
    drawCircle(color = fill, radius = radiusPx, center = center)
    drawCircle(
        color = stroke,
        radius = radiusPx,
        center = center,
        style = Stroke(width = strokeWidth),
    )
}
