package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.scenegraph.style.DipoleChrome

internal fun DrawScope.drawDipole(dipole: Dipole, color: Color, sizePx: Float, dash: PathEffect?) {
    if (dipole.start.isAtInfinity || dipole.end.isAtInfinity) {
        warn("skipping Dipole with endpoint at infinity: $dipole")
        return
    }
    val start = cgaToCanvas(dipole.start.ax, dipole.start.ay)
    val end = cgaToCanvas(dipole.end.ax, dipole.end.ay)
    if (dipole.isImaginary) {
        drawMarkerRing(start, color, sizePx, dash)
        drawMarkerRing(end, color, sizePx, dash)
    } else {
        drawCircle(color = color, radius = sizePx / 2, center = start)
        drawCircle(color = color, radius = sizePx / 2, center = end)
    }
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = sizePx * DipoleChrome.WIDTH_RATIO.toFloat(),
        pathEffect = dash,
    )
}
