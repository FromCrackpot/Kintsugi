package fromcrackpot.kintsugi.scenegraph

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import kotlin.math.sqrt

/**
 * An axis-aligned rectangle in CGA model space — the region a scene or sub-tree occupies.
 * A scene root carries one as its viewport: renderers paint into it and clip at it.
 *
 * Components are raw `Double`s in model units, and equality is the generated structural
 * one — raw comparison of all four components, with no `SCALE` rounding.
 *
 * @constructor Builds the rectangle from its minimum corner and extents.
 * @throws IllegalArgumentException when [width] or [height] is not strictly positive — the
 *   raw value is compared, with no `SCALE` rounding.
 */
public data class SceneBounds(
    /** The left edge — the smallest x the region covers. */
    val minX: Double,
    /** The bottom edge — the smallest y the region covers (model space is Y-up). */
    val minY: Double,
    /** The horizontal extent; strictly positive. */
    val width: Double,
    /** The vertical extent; strictly positive. */
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
 * The polygon and the resulting bounds both live in CGA model space, so this is a plain
 * min/extent read of the raw vertex coordinates — it lets a scene rectangle double as the
 * viewport with no separately maintained bounds constant.
 *
 * @param polygon The polygon to bound.
 * @return The tightest [SceneBounds] covering every vertex.
 * @throws IllegalArgumentException when the vertices span zero raw width or height — a
 *   collinear axis-aligned loop has no rectangular extent.
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
 * Returns the circle centered at the model-space origin that circumscribes an
 * origin-centered [width] × [height] rectangle.
 *
 * Its radius is the rectangle's half-diagonal, so the rectangle lies entirely inside the
 * circle: any line crossing the rectangle also crosses the circle, and the chord between
 * the crossing points covers the line's whole visible span. Renderers clip an infinite
 * line by meeting it with this circle and drawing the chord between the resulting
 * dipole's endpoints.
 *
 * @param width The rectangle's horizontal extent.
 * @param height The rectangle's vertical extent.
 * @return The circumscribing [Circle], classic orientation.
 */
public fun worldBorder(width: Double, height: Double): Circle {
    val hw = width / 2.0
    val hh = height / 2.0
    return Circle(0.0, 0.0, sqrt(hw * hw + hh * hh))
}

/**
 * Returns the circle centered at the model-space origin that circumscribes this bounds'
 * width × height rectangle.
 *
 * Delegates to the two-argument `worldBorder`, ignoring [SceneBounds.minX] and
 * [SceneBounds.minY]: the circle sits at the origin regardless of the bounds' offset — the
 * renderers' offset-ignoring clip convention.
 *
 * @receiver The bounds whose extents size the circle.
 * @return The circumscribing [Circle], classic orientation.
 */
public fun SceneBounds.worldBorder(): Circle = worldBorder(width, height)
