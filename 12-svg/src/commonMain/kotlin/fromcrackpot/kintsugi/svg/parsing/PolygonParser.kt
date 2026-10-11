package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KFLAG_INSIDE_OUT
import fromcrackpot.kintsugi.svg.MalformedPolygonException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.parseKFlags
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/** SVG `points` list separators: whitespace and/or commas (SVG 1.1 points grammar). */
private val POINTS_SEPARATORS = Regex("[\\s,]+")

private val KNOWN_POLYGON_FLAGS = setOf(KFLAG_INSIDE_OUT)

/**
 * Parses a `<polygon>` element (cursor on its START_ELEMENT) into a `PolygonNode` —
 * the untagged (and only) reading; an unknown `k:type` warns and falls back. `points` is
 * **required**: whitespace/comma-separated coordinate pairs, even count, at least 3 pairs.
 * Vertices map through the Y-flip one by one.
 *
 * Orientation rides `k:flags="inside-out"` as intent, model winding as truth: after the
 * Y-flip the vertex walk is reversed when its winding contradicts the flag — flagged loops
 * come out clockwise (inside-out), unflagged loops counter-clockwise (classic) — so an
 * authored `points` order is canonicalized once at parse and a serialized document re-parses
 * without any reversal. A zero-area loop has no orientation to flip; the flag warns and is
 * ignored there.
 *
 * Degenerate vertex lists collapse in `:cga` — [DegeneratePolygonException] wraps into
 * [MalformedPolygonException] at this boundary, per the exception-wrapping contract.
 */
internal fun XmlReader.parsePolygonElement(viewBox: SceneBounds, inherited: InheritedStyle): PolygonNode {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <polygon>${loc?.let { " at $it" } ?: ""}; treating as a polygon")
    }
    val insideOut = KFLAG_INSIDE_OUT in parseKFlags(kAttr(KATTR_FLAGS), KNOWN_POLYGON_FLAGS, "polygon", loc)

    val raw = getAttributeValue(null, "points")
        ?: throw MalformedPolygonException("missing required attribute points", loc)
    val numbers = raw.trim().split(POINTS_SEPARATORS).filter { it.isNotEmpty() }.map {
        it.toDoubleOrNull()
            ?: throw MalformedPolygonException("non-numeric coordinate \"$it\" in points", loc)
    }
    if (numbers.size % 2 != 0) {
        throw MalformedPolygonException("points needs an even coordinate count, got ${numbers.size}", loc)
    }

    val vertices = numbers.chunked(2).map { (sx, sy) ->
        val (cgaX, cgaY) = svgToCga(sx, sy, viewBox)
        FlatPoint(cgaX, cgaY)
    }

    val parsed = try {
        Polygon(vertices)
    } catch (e: DegeneratePolygonException) {
        throw MalformedPolygonException(e.reason, loc)
    }
    val polygon = when {
        insideOut == parsed.isInsideOut -> parsed
        parsed.signedArea == 0.0 -> {
            warn(
                "k:flags=\"$KFLAG_INSIDE_OUT\" on a zero-area <polygon>${loc?.let { " at $it" } ?: ""} " +
                        "has no orientation to flip; ignoring"
            )
            parsed
        }
        else -> -parsed
    }

    val style = SvgDefaults.style(
        fill = readColorAttr("fill", loc) ?: inherited.fill,
        stroke = readColorAttr("stroke", loc) ?: inherited.stroke,
        strokeWidth = attrAsDouble("stroke-width") ?: inherited.strokeWidth,
    )
    return PolygonNode(polygon, style, inherited.tags + kTags())
}
