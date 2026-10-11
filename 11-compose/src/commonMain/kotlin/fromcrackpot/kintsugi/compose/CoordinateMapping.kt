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
 * Model space is centered and Y-up; the canvas is top-left-origin and Y-down. The conversion
 * places the model origin at the canvas center, flips Y, and multiplies by [scale] — scaling
 * about the center, so the origin stays put. Coordinates narrow from `Double` to `Offset`'s
 * `Float` here, at the draw boundary. The mapping is canvas-size-relative: a position converted
 * against one [size] does not transfer to another.
 *
 * @param cgaX The model-space x coordinate.
 * @param cgaY The model-space y coordinate.
 * @param size The canvas size in physical px.
 * @param scale Physical px per CGA unit; the default `1f` reads model units as px directly.
 * @return The canvas position in physical px.
 */
public fun cgaToCanvas(cgaX: Double, cgaY: Double, size: Size, scale: Float = 1f): Offset = Offset(
    size.width / 2f + cgaX.toFloat() * scale,
    size.height / 2f - cgaY.toFloat() * scale,
)

/**
 * Returns the model-space [FlatPoint] under the canvas [position] — the pointer-input inverse
 * of [cgaToCanvas].
 *
 * The inverse mapping against the same [size] and [scale]: subtract the canvas center, flip Y,
 * divide by [scale] — computed raw in `Double` from the `Float` position. Pass the scale the
 * draw pass actually used, [fitScale] over the same surface size, scene bounds, and density —
 * a mismatched scale lands converted positions beside the shapes they point at.
 *
 * @param position The canvas position in physical px.
 * @param size The canvas size in physical px.
 * @param scale Physical px per CGA unit; the default `1f` reads px as model units directly.
 * @return The [FlatPoint] at that position in model space.
 */
public fun canvasToCga(position: Offset, size: Size, scale: Float = 1f): FlatPoint = FlatPoint(
    (position.x - size.width / 2.0) / scale,
    (size.height / 2.0 - position.y) / scale,
)

/**
 * Returns the draw scale — physical px per CGA unit — fitting a scene with [bounds] onto a
 * surface of [size]: the one value the draw pass and pointer-input conversion must share.
 *
 * The scale is uniform: the bounds rectangle fits the surface whole, aspect preserved, so a
 * mismatched surface letterboxes rather than stretches. Null [bounds] falls back to the
 * display [density], one CGA unit per dp on screen.
 *
 * @param size The surface size in physical px.
 * @param bounds The scene's [SceneBounds], or `null` when the scene declares none.
 * @param density The display density (physical px per dp) — the null-bounds fallback scale.
 * @return The uniform draw scale in physical px per CGA unit.
 */
public fun fitScale(size: Size, bounds: SceneBounds?, density: Float): Float = bounds?.let {
    minOf(size.width / it.width.toFloat(), size.height / it.height.toFloat())
} ?: density

/**
 * Returns the world-border circle infinite shapes are clipped against, for a surface of [size]
 * showing a scene with [bounds] at the draw [scale].
 *
 * With declared bounds this is the scene's own circumscribed circle — `SceneBounds.worldBorder()`,
 * origin-centered, ignoring the bounds' offset — so the visible span of an infinite line is a
 * property of the scene, not of the surface showing it. Null [bounds] circumscribes the visible
 * viewport instead, `size / scale` in CGA units: with no declared bounds the surface is the
 * world, and lines run edge-to-edge.
 *
 * @param size The surface size in physical px.
 * @param bounds The scene's [SceneBounds], or `null` when the scene declares none.
 * @param scale The draw scale in physical px per CGA unit — the matching [fitScale] value.
 * @return The clip [Circle] in model space, classic orientation.
 */
public fun worldBorder(size: Size, bounds: SceneBounds?, scale: Float): Circle =
    bounds?.worldBorder()
        ?: worldBorder(size.width / scale.toDouble(), size.height / scale.toDouble())
