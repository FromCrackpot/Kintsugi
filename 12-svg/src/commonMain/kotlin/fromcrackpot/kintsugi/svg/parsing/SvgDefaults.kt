package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * SVG 1.1 spec defaults for omitted style attributes. Used by the parser to materialize
 * a fully explicit [ShapeStyle] — after parse, no field in the resulting style is left
 * "implicit," which means downstream renderers never need to re-derive SVG semantics.
 * Compare with `:scene-graph`'s `ShapeStyle.defaultFor`, which fills in the sensible-for-CGA
 * defaults at the shape level when no style was specified at all.
 */
internal object SvgDefaults {
    // Per SVG 1.1: <rect>, <circle> default fill = black (opaque), stroke = none, stroke-width = 1.
    val FILL: ShapeColor = ShapeColor.BLACK
    val STROKE: ShapeColor = ShapeColor.NONE
    const val STROKE_WIDTH: Double = 1.0

    // Per SVG 1.1: stroke-linecap initial value is `butt`.
    val STROKE_CAP: ShapeStrokeCap = ShapeStrokeCap.BUTT

    /**
     * Materialize a concrete [ShapeStyle] for a fill-bearing element: absent fill falls back to
     * SVG 1.1's black, absent stroke to none, absent stroke-width to 1, absent cap to butt.
     */
    fun style(
        fill: ShapeColor?,
        stroke: ShapeColor?,
        strokeWidth: Double?,
        strokeCap: ShapeStrokeCap? = null,
    ): ShapeStyle = ShapeStyle(
        fill = fill ?: FILL,
        stroke = stroke ?: STROKE,
        strokeWidth = strokeWidth ?: STROKE_WIDTH,
        strokeCap = strokeCap ?: STROKE_CAP,
    )

    /**
     * Materialize a concrete [ShapeStyle] for a `<line>`-family element, which ignores `fill`
     * per SVG semantics — fill stays [ShapeColor.NONE].
     */
    fun strokedStyle(
        stroke: ShapeColor?,
        strokeWidth: Double?,
        strokeCap: ShapeStrokeCap?,
    ): ShapeStyle = ShapeStyle(
        stroke = stroke ?: STROKE,
        strokeWidth = strokeWidth ?: STROKE_WIDTH,
        strokeCap = strokeCap ?: STROKE_CAP,
    )
}
