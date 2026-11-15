package fromcrackpot.kintsugi.compose.drawing.composites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.round

internal fun DrawScope.drawSegment(segment: Segment, stroke: Color, strokeWidth: Float, cap: StrokeCap) {
    if (segment.toDipole.isAtInfinity) {
        warn("skipping Segment at infinity: $segment")
        return
    }
    if (segment.isImaginary) {
        warn("skipping imaginary Segment (radius² < 0): $segment")
        return
    }
    if (round(segment.length) == 0.0) {
        warn("skipping zero-length Segment: $segment")
        return
    }
    drawLine(
        color = stroke,
        start = cgaToCanvas(segment.start.ax, segment.start.ay),
        end = cgaToCanvas(segment.end.ax, segment.end.ay),
        strokeWidth = strokeWidth,
        cap = cap,
    )
}
