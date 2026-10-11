package fromcrackpot.kintsugi.scenegraph.style

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*

/**
 * The concrete paint a shape renders with — fill and stroke colors, stroke geometry, and
 * the open-stroke line cap. Every field holds a definite value; there is no "unspecified"
 * state and no cascade.
 *
 * "No paint" is spelled [ShapeColor.NONE] and "no thickness" `0.0`, which is what the bare
 * constructor defaults every field to: `ShapeStyle(fill = X)` leaves [size] and
 * [strokeWidth] at zero — a visible color on an invisibly sized shape. Start from a
 * per-shape constant and `copy` it (`ShapeStyle.POINT.copy(fill = X)`) unless invisibility
 * is meant.
 */
public data class ShapeStyle(
    /** The interior paint — also the marker color of point-like shapes; [ShapeColor.NONE] paints nothing. */
    val fill: ShapeColor = ShapeColor.NONE,
    /** The outline paint; [ShapeColor.NONE] paints nothing. */
    val stroke: ShapeColor = ShapeColor.NONE,
    /** The outline thickness, renderer-interpreted; `0.0` paints nothing. */
    val strokeWidth: Double = 0.0,
    /** The rendered extent of point markers — side or diameter, renderer-interpreted; `0.0` paints nothing. */
    val size: Double = 0.0,
    /** How open strokes terminate — inert on closed shapes; [ShapeStrokeCap.BUTT] by default. */
    val strokeCap: ShapeStrokeCap = ShapeStrokeCap.BUTT,
) {
    public companion object {
        /**
         * The fully invisible style — no fill, no stroke, zero width and size. A shape
         * rendered with it draws nothing; for visible per-shape defaults use [defaultFor].
         */
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
         * Returns the default style for [shape]'s concrete type — black ink, so an
         * unstyled shape is visible.
         *
         * Points and dipoles get the filled-marker defaults ([POINT], [DIPOLE]); lines,
         * segments, circles, polygons, and polylines the stroked ones ([LINE], [SEGMENT],
         * [CIRCLE], [POLYGON], [POLYLINE]). A [Shape] outside the known categories gets
         * [NONE].
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
 * The shared convention for rendering a dipole's connector — the dashed line joining its
 * endpoint markers. A single source consumed by every renderer, so the same dipole reads
 * the same way everywhere.
 *
 * The connector is not styled separately: its color is the dipole style's [ShapeStyle.fill]
 * (a dipole's endpoint markers are fill-painted, so fill is its single color), its stroke
 * width is the style's [ShapeStyle.size] times [WIDTH_RATIO], and its dash pattern
 * alternates [DASH_ON] units painted with [DASH_OFF] units skipped, in the renderer's
 * units.
 */
public object DipoleChrome {
    /** The painted run of the connector's dash pattern, in the renderer's units. */
    public const val DASH_ON: Double = 2.0

    /** The skipped gap of the connector's dash pattern, in the renderer's units. */
    public const val DASH_OFF: Double = 1.0

    /** The connector's stroke width as a fraction of the dipole style's `size`. */
    public const val WIDTH_RATIO: Double = 0.25
}
