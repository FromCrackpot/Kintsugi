package fromcrackpot.kintsugi.scenegraph.style

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*

/**
 * The concrete paint a shape renders with: fill and stroke colors, stroke geometry, the open-stroke
 * line cap and the dash pattern.
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
    /**
     * The dash pattern of every stroke the shape draws, a dipole's connector included, or `null` when unset.
     *
     * - Imaginary shapes and dipoles replace an unset dash with a default. [dashFor] resolves it.
     * - [ShapeDash.SOLID] keeps every stroke solid, imaginary shapes included.
     */
    val dash: ShapeDash? = null,
) {
    /**
     * Returns the dash pattern [shape]'s strokes draw with under this style, or `null` when they draw solid.
     *
     * - Precedence: [dash], then [ImaginaryChrome.DASH] when imaginary, then [DipoleChrome.DASH] on a dipole.
     *
     * @param shape The shape the style paints.
     * @return The pattern to draw, never [ShapeDash.SOLID], or `null` for a solid stroke.
     */
    public fun dashFor(shape: Shape): ShapeDash? {
        val resolved = dash ?: when {
            ImaginaryChrome.appliesTo(shape) -> ImaginaryChrome.DASH
            shape is Dipole -> DipoleChrome.DASH
            else -> null
        }
        return resolved?.takeUnless { it.isSolid }
    }

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
 * - Dash: the style's [ShapeStyle.dash] when set; else [ImaginaryChrome.DASH] when imaginary; else [DASH].
 */
public object DipoleChrome {
    /** The connector's dash pattern on a real dipole with no dash of its own. */
    public val DASH: ShapeDash = ShapeDash(listOf(2.0, 1.0))

    /** The connector's stroke width as a fraction of the dipole style's `size`. */
    public const val WIDTH_RATIO: Double = 0.25
}

/**
 * The shared convention for drawing imaginary shapes: circles, dipoles, segments and round points
 * whose squared radius is negative. Every renderer draws them the same way.
 *
 * - Strokes take [DASH] unless the style sets its own [ShapeStyle.dash]. Colors stay the style's own.
 * - Point markers become rings of the marker's size, stroked in the fill color, [RING_WIDTH_RATIO] of it wide.
 */
public object ImaginaryChrome {
    /** The dash pattern of an imaginary shape's strokes when its style sets none. */
    public val DASH: ShapeDash = ShapeDash(listOf(4.0, 2.0))

    /** A ring marker's stroke width as a fraction of the style's `size`. */
    public const val RING_WIDTH_RATIO: Double = 0.25

    /**
     * Returns whether [shape] draws in the imaginary style.
     *
     * @param shape The shape to test.
     * @return `true` for a circle, dipole, segment or round point whose squared radius rounds below zero.
     */
    public fun appliesTo(shape: Shape): Boolean = when (shape) {
        is Circle -> shape.isImaginary
        is Dipole -> shape.isImaginary
        is Segment -> shape.isImaginary
        is RoundPoint -> shape.isImaginary
        else -> false
    }
}
