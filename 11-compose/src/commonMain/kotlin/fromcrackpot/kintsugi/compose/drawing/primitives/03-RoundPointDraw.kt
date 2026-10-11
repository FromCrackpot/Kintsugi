package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint

internal fun DrawScope.drawRoundPoint(point: RoundPoint, color: Color, sizePx: Float) {
    if (point.isAtInfinity) {
        warn("skipping RoundPoint at infinity: $point")
        return
    }
    drawCircle(
        color = color,
        radius = sizePx / 2,
        center = cgaToCanvas(point.ax, point.ay),
    )
}
