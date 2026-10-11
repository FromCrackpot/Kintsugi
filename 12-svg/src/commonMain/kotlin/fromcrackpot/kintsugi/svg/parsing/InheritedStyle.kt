package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kTags
import nl.adaptivity.xmlutil.XmlReader

/**
 * The presentation attributes (`fill`, `stroke`, `stroke-width`, `stroke-linecap`) inherited from
 * ancestor `<g>`/`<svg>` elements — SVG's attribute-inheritance model, materialized. Per-property
 * resolution order at each shape: own attr → this inherited chain (nearest ancestor first) →
 * [SvgDefaults]. Resolved at parse time into each shape's concrete
 * [fromcrackpot.kintsugi.scenegraph.style.ShapeStyle]; **NOT** stored on
 * [fromcrackpot.kintsugi.scenegraph.nodes.GroupNode] (which stays purely structural per `:scene-graph`'s
 * "no style cascade" rule). Scoped to the attributes each element's parser already consumes
 * (flat-point rect→fill; rectangle→fill/stroke/stroke-width; round-point→fill;
 * circle→fill/stroke/stroke-width; line→stroke/stroke-width/stroke-linecap).
 */
internal data class InheritedStyle(
    val fill: ShapeColor? = null,
    val stroke: ShapeColor? = null,
    val strokeWidth: Double? = null,
    val strokeCap: ShapeStrokeCap? = null,
    val tags: Set<String> = emptySet(),
) {
    /**
     * The inherited style a child sees: the child element's own attrs overlaid on this chain.
     * Style fields resolve nearest-wins (`?:`); [tags] **union** instead — a leaf accumulates
     * every ancestor's `k:tags` (baked into the leaf at parse, like the style cascade — never
     * stored on `GroupNode`).
     */
    fun child(own: InheritedStyle): InheritedStyle =
        InheritedStyle(
            fill = own.fill ?: fill,
            stroke = own.stroke ?: stroke,
            strokeWidth = own.strokeWidth ?: strokeWidth,
            strokeCap = own.strokeCap ?: strokeCap,
            tags = tags + own.tags,
        )
}

/**
 * Read the four inheritable presentation attributes off the current element (`<svg>` root or a
 * `<g>`) into an [InheritedStyle] layer. Used to seed the chain at the root and to overlay each
 * nested `<g>`'s own attrs via [InheritedStyle.child].
 */
internal fun XmlReader.readInheritedStyle(location: String?): InheritedStyle =
    InheritedStyle(
        fill = readColorAttr("fill", location),
        stroke = readColorAttr("stroke", location),
        strokeWidth = attrAsDouble("stroke-width"),
        strokeCap = readStrokeCapAttr("stroke-linecap", location),
        tags = kTags(),
    )
