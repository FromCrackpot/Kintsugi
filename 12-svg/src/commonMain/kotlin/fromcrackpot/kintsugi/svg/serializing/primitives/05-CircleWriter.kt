package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KFLAG_IMAGINARY
import fromcrackpot.kintsugi.svg.KFLAG_INSIDE_OUT
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kChromeAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.strokeDashAttrs
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Circle → plain `<circle>` (no `k:type`; designer-authored circles "just work"). `cx`/`cy` are the
 * geometric center, `r = √|radius²|`. Imaginary (`radius² < 0`) and inside-out (`cw > 0`) circles are
 * **persisted** (not skipped) via `k:flags`: faithful CGA storage.
 *
 * - An imaginary circle with no dash of its own writes `ImaginaryChrome.DASH`, listed in `k:chrome`,
 *   so viewers draw it the way the scene renders it.
 */
internal fun XmlWriter.writeCircleElement(shape: CircleNode, viewBox: SceneBounds) {
    val circle: Circle = shape
    val style = shape.style

    val (svgCx, svgCy) = cgaToSvg(circle.center.ax, circle.center.ay, viewBox)
    val flags = buildList {
        if (circle.isImaginary) add(KFLAG_IMAGINARY)
        if (circle.cw > 0) add(KFLAG_INSIDE_OUT)
    }

    startTag(SVG_NS, "circle", "")
    attribute(null, "cx", null, svgCx.formatSvgNumber())
    attribute(null, "cy", null, svgCy.formatSvgNumber())
    attribute(null, "r", null, circle.radius.formatSvgNumber())
    colorAttr("fill", style.fill)
    colorAttr("stroke", style.stroke)
    attribute(null, "stroke-width", null, style.strokeWidth.formatSvgNumber())
    val chrome = mutableListOf<String>()
    strokeDashAttrs(style, circle, chrome)
    if (flags.isNotEmpty()) kAttr(KATTR_FLAGS, flags.joinToString(" "))
    kChromeAttr(chrome)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "circle", "")
}
