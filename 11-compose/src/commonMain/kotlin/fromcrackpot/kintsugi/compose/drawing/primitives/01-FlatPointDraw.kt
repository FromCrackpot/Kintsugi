package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint

internal fun DrawScope.drawFlatPoint(point: FlatPoint, color: Color, sizePx: Float) {
    if (point.isAtInfinity) {
        warn("skipping FlatPoint at infinity: $point")
        return
    }
    val center = cgaToCanvas(point.px, point.py)
    val half = sizePx / 2
    drawRect(
        color = color,
        topLeft = Offset(center.x - half, center.y - half),
        size = Size(sizePx, sizePx),
    )
}
