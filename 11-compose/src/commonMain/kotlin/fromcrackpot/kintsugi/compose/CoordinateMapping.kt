package fromcrackpot.kintsugi.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.worldBorder

/**
 * Returns the canvas position of the model-space point ([cgaX], [cgaY]).
 *
 * - The result depends on the canvas size. A position converted against one [size] doesn't transfer to another.
 *
 * @param cgaX The model-space x coordinate.
 * @param cgaY The model-space y coordinate.
 * @param size The canvas size in physical px.
 * @param scale Physical px per CGA unit. The default `1f` reads model units as px directly.
 * @return The canvas position in physical px.
 */
public fun cgaToCanvas(cgaX: Double, cgaY: Double, size: Size, scale: Float = 1f): Offset = Offset(
    size.width / 2f + cgaX.toFloat() * scale,
    size.height / 2f - cgaY.toFloat() * scale,
)

/**
 * Returns the model-space [FlatPoint] under the canvas [position]. It is the pointer-input
 * inverse of [cgaToCanvas].
 *
 * - Pass the [fitScale] the draw pass used, or converted positions land beside the shapes.
 *
 * @param position The canvas position in physical px.
 * @param size The canvas size in physical px.
 * @param scale Physical px per CGA unit. The default `1f` reads px as model units directly.
 * @return The point at that position in model space.
 */
public fun canvasToCga(position: Offset, size: Size, scale: Float = 1f): FlatPoint = FlatPoint(
    (position.x - size.width / 2.0) / scale,
    (size.height / 2.0 - position.y) / scale,
)

/**
 * Returns the draw scale, in physical px per CGA unit, that fits a scene with [bounds] onto a
 * surface of [size]. The draw pass and pointer-input conversion must share this one value.
 *
 * - The scale is uniform, preserving aspect and letterboxing on mismatch. Null [bounds] fall back to one unit per dp.
 *
 * @param size The surface size in physical px.
 * @param bounds The scene's bounds, or `null` when the scene declares none.
 * @param density The display density in physical px per dp, used as the null-bounds fallback scale.
 * @return The uniform draw scale in physical px per CGA unit.
 */
public fun fitScale(size: Size, bounds: SceneBounds?, density: Float): Float = bounds?.let {
    minOf(size.width / it.width.toFloat(), size.height / it.height.toFloat())
} ?: density

/**
 * Returns the world-border circle infinite shapes are clipped against, for a surface of [size]
 * showing a scene with [bounds] at the draw [scale].
 *
 * - With bounds, the scene's own border; without, the circle around the visible viewport.
 *
 * @param size The surface size in physical px.
 * @param bounds The scene's bounds, or `null` when the scene declares none.
 * @param scale The draw scale in physical px per CGA unit, the matching [fitScale] value.
 * @return The clip circle in model space, classic orientation.
 */
public fun worldBorder(size: Size, bounds: SceneBounds?, scale: Float): Circle =
    bounds?.worldBorder()
        ?: worldBorder(size.width / scale.toDouble(), size.height / scale.toDouble())
