package fromcrackpot.kintsugi.scenegraph

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import kotlin.math.sqrt

/**
 * An axis-aligned rectangle in CGA model space, giving the region a scene or sub-tree occupies. On a
 * scene root, it is the viewport renderers paint into and clip at.
 *
 * - Stores raw components. Equality is exact, with no `SCALE` rounding.
 *
 * @constructor Builds the rectangle from its minimum corner and extents.
 * @throws IllegalArgumentException when [width] or [height] is not strictly positive.
 */
public data class SceneBounds(
    /** The left edge, the smallest x the region covers. */
    val minX: Double,
    /** The bottom edge, the smallest y the region covers. Model space is Y-up. */
    val minY: Double,
    /** The horizontal extent, strictly positive. */
    val width: Double,
    /** The vertical extent, strictly positive. */
    val height: Double,
) {
    init {
        require(width > 0.0) { "bounds width must be > 0, was $width" }
        require(height > 0.0) { "bounds height must be > 0, was $height" }
    }
}

/**
 * Returns the axis-aligned bounding box of [polygon]'s vertices.
 *
 * @param polygon The polygon to bound.
 * @return The tightest bounds covering every vertex.
 * @throws IllegalArgumentException when the vertices span zero raw width or height.
 */
public fun SceneBounds(polygon: Polygon): SceneBounds {
    val minX = polygon.vertices.minOf { it.px }
    val minY = polygon.vertices.minOf { it.py }
    return SceneBounds(
        minX = minX,
        minY = minY,
        width = polygon.vertices.maxOf { it.px } - minX,
        height = polygon.vertices.maxOf { it.py } - minY,
    )
}

/**
 * Returns the origin-centered circle circumscribing an origin-centered [width] × [height]
 * rectangle.
 *
 * - Renderers clip an infinite line to the chord where it meets this circle.
 *
 * @param width The rectangle's horizontal extent.
 * @param height The rectangle's vertical extent.
 * @return The circumscribing classic circle.
 */
public fun worldBorder(width: Double, height: Double): Circle {
    val hw = width / 2.0
    val hh = height / 2.0
    return Circle(0.0, 0.0, sqrt(hw * hw + hh * hh))
}

/**
 * Returns the origin-centered circle circumscribing this bounds' width × height rectangle.
 *
 * - Ignores [SceneBounds.minX] and [SceneBounds.minY]. The circle sits at the origin regardless.
 *
 * @receiver The bounds whose extents size the circle.
 * @return The circumscribing classic circle.
 */
public fun SceneBounds.worldBorder(): Circle = worldBorder(width, height)
