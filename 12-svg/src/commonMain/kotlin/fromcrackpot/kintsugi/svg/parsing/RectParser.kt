package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KFLAG_INSIDE_OUT
import fromcrackpot.kintsugi.svg.KTYPE_FLAT_POINT
import fromcrackpot.kintsugi.svg.MalformedRectException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.parseKFlags
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

private val KNOWN_RECT_FLAGS = setOf(KFLAG_INSIDE_OUT)

/**
 * Parses a `<rect>` element (cursor on its START_ELEMENT). The `k:type` fork:
 *
 * - `k:type="flat-point"` → [FlatPoint] (the tagged reading; square rect, side = render size).
 * - untagged (or unknown `k:type`, which warns) → rectangle [Polygon] — the untagged default,
 *   mirroring bare `<circle>` → Circle and bare `<line>` → Segment. **Parse-only**: there is no
 *   Rectangle type in `:cga`; the element decodes through `Polygon.Rectangle` and re-serializes
 *   as `<polygon>`. There is no background reading — a full-bleed rect is just a rectangle
 *   Polygon that happens to cover the viewBox.
 */
internal fun XmlReader.parseRectElement(viewBox: SceneBounds, inherited: InheritedStyle): ShapeNode {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)
    val x = attrAsDouble("x")
    val y = attrAsDouble("y")
    val width = attrAsDouble("width")
    val height = attrAsDouble("height")
    val ownFill = readColorAttr("fill", loc)
    val tags = inherited.tags + kTags()

    if (kType == KTYPE_FLAT_POINT) {
        return flatPointFrom(
            x = x, y = y, width = width, height = height,
            fill = ownFill ?: inherited.fill, tags = tags, viewBox = viewBox, location = loc,
        )
    }
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <rect>${loc?.let { " at $it" } ?: ""}; treating as a rectangle")
    }
    return rectangleFrom(
        x = x, y = y, width = width, height = height,
        rx = attrAsDouble("rx"), ry = attrAsDouble("ry"),
        flags = kAttr(KATTR_FLAGS),
        fill = ownFill ?: inherited.fill,
        stroke = readColorAttr("stroke", loc) ?: inherited.stroke,
        strokeWidth = attrAsDouble("stroke-width") ?: inherited.strokeWidth,
        tags = tags, viewBox = viewBox, location = loc,
    )
}

/**
 * Builds a `FlatPointNode` from the attributes of a `<rect k:type="flat-point">`.
 *
 * The rect must be **square** (`width == height`); its side length *is* the render marker size
 * (`ShapeStyle.size`). CGA position is the rect center (`x + width/2`, `y + height/2`). A non-square
 * flat-point rect is malformed (a point has a single `size`).
 */
private fun flatPointFrom(
    x: Double?,
    y: Double?,
    width: Double?,
    height: Double?,
    fill: ShapeColor?,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): FlatPointNode {
    val w = width ?: throw MalformedRectException("missing required attribute width", location)
    val h = height ?: throw MalformedRectException("missing required attribute height", location)
    if (w != h) {
        throw MalformedRectException("flat-point <rect> must be square (width == height), was width=$w height=$h", location)
    }
    if (w <= 0.0) {
        throw MalformedRectException("flat-point <rect> size must be > 0, was $w", location)
    }
    val sx = x ?: 0.0
    val sy = y ?: 0.0
    val (cgaX, cgaY) = svgToCga(sx + w / 2.0, sy + h / 2.0, viewBox)

    val style = ShapeStyle(
        fill = fill ?: SvgDefaults.FILL,
        size = w,
    )
    return FlatPointNode(FlatPoint(cgaX, cgaY), style, tags)
}

/**
 * Builds a `PolygonNode` from the attributes of a bare `<rect>` (no `k:type`).
 *
 * All four geometry attributes are **required** — deliberately stricter than flat-point/circle's
 * default-absent-position-to-0 posture: a designer `<rect>` without extents is a mistake, not a
 * degenerate default. Non-zero `rx`/`ry` (rounded corners) are unsupported geometry and throw
 * rather than silently flattening the corners on load; zero/absent are fine.
 * `k:flags="inside-out"` reverses the factory's counter-clockwise walk, so the flagged rect
 * decodes as an inside-out rectangle Polygon — the wall reading.
 *
 * Zero/negative extents collapse in `:cga` — [DegeneratePolygonException] wraps into
 * [MalformedRectException] at this boundary (rect vocabulary — the caller authored a `<rect>`),
 * per the exception-wrapping contract.
 */
private fun rectangleFrom(
    x: Double?,
    y: Double?,
    width: Double?,
    height: Double?,
    rx: Double?,
    ry: Double?,
    flags: String?,
    fill: ShapeColor?,
    stroke: ShapeColor?,
    strokeWidth: Double?,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): PolygonNode {
    val sx = x ?: throw MalformedRectException("missing required attribute x", location)
    val sy = y ?: throw MalformedRectException("missing required attribute y", location)
    val w = width ?: throw MalformedRectException("missing required attribute width", location)
    val h = height ?: throw MalformedRectException("missing required attribute height", location)
    if ((rx != null && rx != 0.0) || (ry != null && ry != 0.0)) {
        throw MalformedRectException("rounded corners (rx/ry) not supported", location)
    }
    // Negative extents are an SVG error (and must not silently re-anchor: the corner factory's
    // min/max normalization would happily build the flipped rect); zero collapses, caught below.
    if (w < 0.0 || h < 0.0) {
        throw MalformedRectException("width and height must be > 0, was width=$w height=$h", location)
    }

    // SVG top-left (Y-down) and bottom-right map to the CGA bottom-left / top-right corners
    // through the Y-flip; the factory normalizes, so any opposite pair works.
    val (blX, blY) = svgToCga(sx, sy + h, viewBox)
    val (trX, trY) = svgToCga(sx + w, sy, viewBox)

    val insideOut = KFLAG_INSIDE_OUT in parseKFlags(flags, KNOWN_RECT_FLAGS, "rect", location)
    val polygon = try {
        Polygon.Rectangle(FlatPoint(blX, blY), FlatPoint(trX, trY)).let { if (insideOut) -it else it }
    } catch (_: DegeneratePolygonException) {
        throw MalformedRectException("width and height must be > 0, was width=$w height=$h", location)
    }

    return PolygonNode(polygon, SvgDefaults.style(fill, stroke, strokeWidth), tags)
}
