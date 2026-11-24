package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.style.ImaginaryChrome

internal fun DrawScope.drawRoundPoint(point: RoundPoint, color: Color, sizePx: Float, dash: PathEffect?) {
    if (point.isAtInfinity) {
        warn("skipping RoundPoint at infinity: $point")
        return
    }
    val center = cgaToCanvas(point.ax, point.ay)
    if (point.isImaginary) drawMarkerRing(center, color, sizePx, dash)
    else drawCircle(color = color, radius = sizePx / 2, center = center)
}

internal fun DrawScope.drawMarkerRing(center: Offset, color: Color, sizePx: Float, dash: PathEffect?) {
    drawCircle(
        color = color,
        radius = sizePx / 2,
        center = center,
        style = Stroke(width = sizePx * ImaginaryChrome.RING_WIDTH_RATIO.toFloat(), pathEffect = dash),
    )
}
