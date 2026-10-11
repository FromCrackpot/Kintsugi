package fromcrackpot.kintsugi.compose.drawing.composites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment

internal fun DrawScope.drawSegment(segment: Segment, stroke: Color, strokeWidth: Float, cap: StrokeCap) {
    // Bounded by construction (two finite FlatPoint endpoints lifted and wedged), so no worldBorder
    // clipping and no `isImaginary` guard — imaginary segments cannot occur by construction.
    drawLine(
        color = stroke,
        start = cgaToCanvas(segment.start.ax, segment.start.ay),
        end = cgaToCanvas(segment.end.ax, segment.end.ay),
        strokeWidth = strokeWidth,
        cap = cap,
    )
}
