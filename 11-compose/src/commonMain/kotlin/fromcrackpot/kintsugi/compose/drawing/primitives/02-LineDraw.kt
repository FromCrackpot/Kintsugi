package fromcrackpot.kintsugi.compose.drawing.primitives

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import fromcrackpot.kintsugi.compose.drawing.cgaToCanvas
import fromcrackpot.kintsugi.compose.warn
import fromcrackpot.kintsugi.cga.d2.operations.products.meet
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line

internal fun DrawScope.drawLine(
    line: Line,
    worldBorder: Circle,
    stroke: Color,
    strokeWidth: Float,
    cap: StrokeCap,
    dash: PathEffect?,
) {
    if (line.isAtInfinity) {
        warn("skipping Line at infinity: $line")
        return
    }

    // `worldBorder ∨ line`, not `line ∨ worldBorder`, so the dipole's start and end land on the
    // line's a-side and b-side in that order. Same-grade antiwedge is antisymmetric: swapping
    // the operands swaps start/end.
    val dipole = worldBorder meet line

    if (dipole.isImaginary) {
        warn("skipping Line outside viewport (imaginary intersection with world border): $line")
        return
    }
    drawLine(
        color = stroke,
        start = cgaToCanvas(dipole.start.ax, dipole.start.ay),
        end = cgaToCanvas(dipole.end.ax, dipole.end.ay),
        strokeWidth = strokeWidth,
        cap = cap,
        pathEffect = dash,
    )
}
