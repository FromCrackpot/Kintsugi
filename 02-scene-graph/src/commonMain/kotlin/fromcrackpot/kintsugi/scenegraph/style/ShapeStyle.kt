package fromcrackpot.kintsugi.scenegraph.style

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*

/**
 * The concrete paint a shape renders with: fill and stroke colors, stroke geometry, and the
 * open-stroke line cap.
 *
 * - The bare constructor defaults everything to nothing, so `ShapeStyle(fill = X)` draws at zero size.
 * - Start from a per-shape constant instead: `ShapeStyle.POINT.copy(fill = X)`.
 */
public data class ShapeStyle(
    /** The interior paint, also the marker color of point-like shapes. [ShapeColor.NONE] paints nothing. */
    val fill: ShapeColor = ShapeColor.NONE,
    /** The outline paint. [ShapeColor.NONE] paints nothing. */
    val stroke: ShapeColor = ShapeColor.NONE,
    /** The outline thickness, as the renderer interprets it. `0.0` paints nothing. */
    val strokeWidth: Double = 0.0,
    /** The rendered extent of point markers, as a side or diameter the renderer interprets. `0.0` paints nothing. */
    val size: Double = 0.0,
    /** How open strokes terminate, inert on closed shapes. [ShapeStrokeCap.BUTT] by default. */
    val strokeCap: ShapeStrokeCap = ShapeStrokeCap.BUTT,
) {
    public companion object {
        /** The fully invisible style: no fill, no stroke, zero width and size. */
        public val NONE: ShapeStyle = ShapeStyle()

        /** The default style for the point primitives. */
        public val POINT: ShapeStyle = ShapeStyle(fill = ShapeColor.BLACK, size = 8.0)

        /** The default style for lines. */
        public val LINE: ShapeStyle = ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0)

        /** The default style for dipoles. */
        public val DIPOLE: ShapeStyle = ShapeStyle(fill = ShapeColor.BLACK, size = 8.0)

        /** The default style for circles. */
        public val CIRCLE: ShapeStyle = ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0)

        /** The default style for segments. */
        public val SEGMENT: ShapeStyle = ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0)

        /** The default style for polylines. */
        public val POLYLINE: ShapeStyle = ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0)

        /** The default style for polygons. */
        public val POLYGON: ShapeStyle = ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0)

        /**
         * Returns the default style for [shape]'s concrete type. It uses black ink, so an unstyled
         * shape is visible.
         *
         * - A [Shape] outside the known categories gets [NONE].
         *
         * @param shape The shape to pick a default for.
         * @return The matching per-shape constant.
         */
        public fun defaultFor(shape: Shape): ShapeStyle = when (shape) {
            is FlatPoint, is RoundPoint -> POINT
            is Line -> LINE
            is Dipole -> DIPOLE
            is Circle -> CIRCLE
            is Segment -> SEGMENT
            is Polyline -> POLYLINE
            is Polygon -> POLYGON
            else -> NONE
        }
    }
}

/**
 * The shared convention for drawing a dipole's connector, the dashed line joining its endpoint
 * markers. Every renderer draws it the same way.
 *
 * - Color: the dipole style's [ShapeStyle.fill]; width: its [ShapeStyle.size] times [WIDTH_RATIO].
 * - Dashes alternate [DASH_ON] painted and [DASH_OFF] skipped, in the renderer's units.
 */
public object DipoleChrome {
    /** The painted run of the connector's dash pattern, in the renderer's units. */
    public const val DASH_ON: Double = 2.0

    /** The skipped gap of the connector's dash pattern, in the renderer's units. */
    public const val DASH_OFF: Double = 1.0

    /** The connector's stroke width as a fraction of the dipole style's `size`. */
    public const val WIDTH_RATIO: Double = 0.25
}
