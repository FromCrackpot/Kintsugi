package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.MalformedPolylineException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/** SVG `points` list separators: whitespace and/or commas (SVG 1.1 points grammar). */
private val POINTS_SEPARATORS = Regex("[\\s,]+")

/**
 * Parses a `<polyline>` element (cursor on its START_ELEMENT) into a `PolylineNode` —
 * the untagged (and only) reading, mirroring `<polygon>`; an unknown `k:type` warns and falls
 * back. `points` shares the polygon grammar but with the open chain's looser floor (≥ 2 pairs;
 * first == last legal — an open chain may revisit points). Traversal order is preserved as data.
 *
 * Unlike polygon, a polyline is an open stroke, so `stroke-linecap` applies (segment's rule)
 * and cascades through the inherited-style chain.
 *
 * Degenerate vertex lists collapse in `:cga` — [DegeneratePolylineException] wraps into
 * [MalformedPolylineException] at this boundary, per the exception-wrapping contract.
 */
internal fun XmlReader.parsePolylineElement(viewBox: SceneBounds, inherited: InheritedStyle): PolylineNode {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <polyline>${loc?.let { " at $it" } ?: ""}; treating as a polyline")
    }

    val raw = getAttributeValue(null, "points")
        ?: throw MalformedPolylineException("missing required attribute points", loc)
    val numbers = raw.trim().split(POINTS_SEPARATORS).filter { it.isNotEmpty() }.map {
        it.toDoubleOrNull()
            ?: throw MalformedPolylineException("non-numeric coordinate \"$it\" in points", loc)
    }
    if (numbers.size % 2 != 0) {
        throw MalformedPolylineException("points needs an even coordinate count, got ${numbers.size}", loc)
    }

    val vertices = numbers.chunked(2).map { (sx, sy) ->
        val (cgaX, cgaY) = svgToCga(sx, sy, viewBox)
        FlatPoint(cgaX, cgaY)
    }

    val polyline = try {
        Polyline(vertices)
    } catch (e: DegeneratePolylineException) {
        throw MalformedPolylineException(e.reason, loc)
    }

    val style = SvgDefaults.style(
        fill = readColorAttr("fill", loc) ?: inherited.fill,
        stroke = readColorAttr("stroke", loc) ?: inherited.stroke,
        strokeWidth = attrAsDouble("stroke-width") ?: inherited.strokeWidth,
        strokeCap = readStrokeCapAttr("stroke-linecap", loc) ?: inherited.strokeCap,
    )
    return PolylineNode(polyline, style, inherited.tags + kTags())
}
