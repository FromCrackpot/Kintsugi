package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.scenegraph.style.DipoleChrome

internal fun DrawScope.drawDipole(dipole: Dipole, color: Color, sizePx: Float) {
    if (dipole.isImaginary) {
        warn("skipping imaginary Dipole (radius² < 0): $dipole")
        return
    }
    val start = cgaToCanvas(dipole.start.ax, dipole.start.ay)
    val end = cgaToCanvas(dipole.end.ax, dipole.end.ay)
    val dotRadius = sizePx / 2
    drawCircle(color = color, radius = dotRadius, center = start)
    drawCircle(color = color, radius = dotRadius, center = end)
    // Chrome line conventions live in :scene-graph's DipoleChrome — :svg's DipoleEncoding consumes them
    // identically, so the same dipole reads the same way in both renderers.
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = sizePx * DipoleChrome.WIDTH_RATIO.toFloat(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DipoleChrome.DASH_ON.toFloat(), DipoleChrome.DASH_OFF.toFloat())),
    )
}
