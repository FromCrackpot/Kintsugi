package fromcrackpot.kintsugi.compose.tracing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToLong

/* DRAW EVENTS */

internal sealed class DrawEvent {
    abstract fun format(): String

    data class Rect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val argb: Int,
        val style: PaintingStyle,
        val strokeWidth: Float,
    ) : DrawEvent() {
        override fun format(): String {
            val width = right - left
            val height = bottom - top
            return "rect    tl=(${fmt(left)}, ${fmt(top)}) size=(${fmt(width)}, ${fmt(height)})  ${paintLabel(style, argb, strokeWidth)}"
        }
    }

    data class Line(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val argb: Int,
        val strokeWidth: Float,
        val dashed: Boolean = false,
        val cap: StrokeCap = StrokeCap.Butt,
    ) : DrawEvent() {
        override fun format(): String {
            val base = "line    p1=(${fmt(x1)}, ${fmt(y1)}) p2=(${fmt(x2)}, ${fmt(y2)})  stroke=${hex(argb)} w=${fmt(strokeWidth)}"
            // Conditional suffixes (mirroring `dash=on`): default Butt cap prints nothing, so every
            // pre-existing golden stays byte-identical; only Round/Square annotate.
            val withDash = if (dashed) "$base dash=on" else base
            return when (cap) {
                StrokeCap.Round -> "$withDash cap=round"
                StrokeCap.Square -> "$withDash cap=square"
                else -> withDash
            }
        }
    }

    data class Circle(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val argb: Int,
        val style: PaintingStyle,
        val strokeWidth: Float,
    ) : DrawEvent() {
        override fun format(): String =
            "circle  c=(${fmt(cx)}, ${fmt(cy)}) r=${fmt(radius)}  ${paintLabel(style, argb, strokeWidth)}"
    }

    /**
     * A `Canvas.drawPath` call, traced by the path's control-point bounding box (the vertex data
     * itself isn't portably extractable from a Compose [androidx.compose.ui.graphics.Path]).
     */
    data class PathBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val argb: Int,
        val style: PaintingStyle,
        val strokeWidth: Float,
    ) : DrawEvent() {
        override fun format(): String {
            val width = right - left
            val height = bottom - top
            return "path    tl=(${fmt(left)}, ${fmt(top)}) size=(${fmt(width)}, ${fmt(height)})  ${paintLabel(style, argb, strokeWidth)}"
        }
    }
}

// Round to 3 decimals to absorb harmless Double→Float jitter from the CGA coordinate transform.
private fun fmt(f: Float): String {
    val rounded = (f.toDouble() * 1000.0).roundToLong() / 1000.0
    val asLong = rounded.toLong()
    return if (rounded == asLong.toDouble()) "$asLong" else "$rounded"
}

private fun hex(argb: Int): String {
    val a = (argb shr 24) and 0xFF
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val hex2 = { v: Int -> v.toString(16).padStart(2, '0') }
    return if (a == 0xFF) "#${hex2(r)}${hex2(g)}${hex2(b)}"
    else "#${hex2(a)}${hex2(r)}${hex2(g)}${hex2(b)}"
}

private fun paintLabel(style: PaintingStyle, argb: Int, strokeWidth: Float): String =
    if (style == PaintingStyle.Stroke) "stroke=${hex(argb)} w=${fmt(strokeWidth)}"
    else "fill=${hex(argb)}"

/* RECORDING CANVAS */

/**
 * Captures draw calls our production `DrawScope.draw<X>` extensions route through. Transform / clip
 * / save / restore are no-ops (CanvasDrawScope uses them around every draw to apply state); draw
 * methods we don't expect (image, arc, vertices, …) throw, so adding a primitive whose
 * extension reaches for an un-recorded API surfaces as a failing test rather than a silent miss.
 * `drawPath` records bounds-only (see [DrawEvent.PathBounds]) — the polygon route.
 */
internal class RecordingCanvas : Canvas {
    val events = mutableListOf<DrawEvent>()

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        events += DrawEvent.Rect(
            left = left, top = top, right = right, bottom = bottom,
            argb = paint.color.toArgb(),
            style = paint.style,
            strokeWidth = paint.strokeWidth,
        )
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        events += DrawEvent.Line(
            x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y,
            argb = paint.color.toArgb(),
            strokeWidth = paint.strokeWidth,
            dashed = paint.pathEffect != null,
            cap = paint.strokeCap,
        )
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        events += DrawEvent.Circle(
            cx = center.x, cy = center.y, radius = radius,
            argb = paint.color.toArgb(),
            style = paint.style,
            strokeWidth = paint.strokeWidth,
        )
    }

    // State stack and transforms: no-ops. CanvasDrawScope wraps each draw to apply the inset/
    // translate/clip from DrawScope.drawContext.transform — we don't need to honor any of it
    // for the trace; the production draw extensions pass already-resolved canvas-space coords.
    override fun save() {}
    override fun restore() {}
    override fun saveLayer(bounds: Rect, paint: Paint) {}
    override fun translate(dx: Float, dy: Float) {}
    override fun scale(sx: Float, sy: Float) {}
    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) {}
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {}
    override fun clipPath(path: Path, clipOp: ClipOp) {}

    // Draw calls our production extensions don't make: throw so an unexpected route is loud.
    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radiusX: Float, radiusY: Float, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawRoundRect")
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawOval")
    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawArc")
    override fun drawPath(path: Path, paint: Paint) {
        val bounds = path.getBounds()
        events += DrawEvent.PathBounds(
            left = bounds.left, top = bounds.top, right = bounds.right, bottom = bounds.bottom,
            argb = paint.color.toArgb(),
            style = paint.style,
            strokeWidth = paint.strokeWidth,
        )
    }
    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawImage")
    override fun drawImageRect(image: ImageBitmap, srcOffset: IntOffset, srcSize: IntSize, dstOffset: IntOffset, dstSize: IntSize, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawImageRect")
    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawPoints")
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawRawPoints")
    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint): Unit =
        throw NotImplementedError("RecordingCanvas saw unexpected drawVertices")
    override fun enableZ() {}
    override fun disableZ() {}
}
