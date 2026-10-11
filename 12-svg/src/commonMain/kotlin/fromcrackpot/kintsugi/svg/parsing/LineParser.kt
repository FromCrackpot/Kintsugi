package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_LINE
import fromcrackpot.kintsugi.svg.MalformedLineException
import fromcrackpot.kintsugi.svg.MalformedSegmentException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/**
 * Parses a `<line>` element (cursor on its START_ELEMENT). The `k:type` fork:
 *
 * - `k:type="line"` → infinite CGA [Line] — recognition is by discriminator, never by endpoint
 *   geometry.
 * - untagged (or unknown `k:type`, which warns) → [Segment] — the untagged default.
 *
 * `<line>` ignores `fill` per SVG semantics; we follow.
 */
internal fun XmlReader.parseLineElement(viewBox: SceneBounds, inherited: InheritedStyle): ShapeNode {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)
    val x1 = attrAsDouble("x1")
    val y1 = attrAsDouble("y1")
    val x2 = attrAsDouble("x2")
    val y2 = attrAsDouble("y2")
    val stroke = readColorAttr("stroke", loc) ?: inherited.stroke
    val strokeWidth = attrAsDouble("stroke-width") ?: inherited.strokeWidth
    val strokeCap = readStrokeCapAttr("stroke-linecap", loc) ?: inherited.strokeCap
    val tags = inherited.tags + kTags()

    if (kType == KTYPE_LINE) {
        return lineFrom(x1, y1, x2, y2, stroke, strokeWidth, strokeCap, tags, viewBox, loc)
    }
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <line>${loc?.let { " at $it" } ?: ""}; treating as a segment")
    }
    return segmentFrom(x1, y1, x2, y2, stroke, strokeWidth, strokeCap, tags, viewBox, loc)
}

/**
 * Builds a `LineNode` from the attributes of a `<line k:type="line">` via
 * `FlatPoint(x1,y1) join FlatPoint(x2,y2)`. Endpoint order encodes the line's orientation.
 *
 * Coincident endpoints are **not** rejected at this boundary: `:cga`'s `join` produces a degenerate
 * `Line` when `start == end`, and the serializer's `isAtInfinity` check handles it downstream.
 * Letting the math speak avoids editorialising a CGA concern from inside the SVG codec.
 */
private fun lineFrom(
    x1: Double?,
    y1: Double?,
    x2: Double?,
    y2: Double?,
    stroke: ShapeColor?,
    strokeWidth: Double?,
    strokeCap: ShapeStrokeCap?,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): LineNode {
    val sx1 = x1 ?: throw MalformedLineException("missing required attribute x1", location)
    val sy1 = y1 ?: throw MalformedLineException("missing required attribute y1", location)
    val sx2 = x2 ?: throw MalformedLineException("missing required attribute x2", location)
    val sy2 = y2 ?: throw MalformedLineException("missing required attribute y2", location)

    val (cx1, cy1) = svgToCga(sx1, sy1, viewBox)
    val (cx2, cy2) = svgToCga(sx2, sy2, viewBox)

    val start = FlatPoint(cx1, cy1)
    val end = FlatPoint(cx2, cy2)

    return LineNode(start join end, SvgDefaults.strokedStyle(stroke, strokeWidth, strokeCap), tags)
}

/**
 * Builds a `SegmentNode` from the attributes of a bare `<line>` (no `k:type`) by lifting
 * the two endpoints to `FlatPoint`s and feeding them into `Segment(FlatPoint, FlatPoint)` — the
 * existing 2-arg ctor does `start.toRound join end.toRound` internally. Endpoint order encodes the
 * segment's orientation (same `start ∧ end` contract as Line, applied at the round-point level).
 *
 * Segments are bounded by construction (two finite Euclidean endpoints), so unlike Line there is
 * no worldBorder clipping at serialize, no `isAtInfinity` guard, and no imaginary case — per
 * `:cga`'s Segment KDoc, "the `(FlatPoint, FlatPoint)` ctor cannot produce an imaginary Segment."
 *
 * Coincident endpoints collapse the underlying Dipole's carrier norm to zero — `:cga`'s ctor raises
 * `DegenerateDipoleException`, which we wrap as `MalformedSegmentException` at this boundary
 * (matches the dipole encoding's posture).
 */
private fun segmentFrom(
    x1: Double?,
    y1: Double?,
    x2: Double?,
    y2: Double?,
    stroke: ShapeColor?,
    strokeWidth: Double?,
    strokeCap: ShapeStrokeCap?,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): SegmentNode {
    val sx1 = x1 ?: throw MalformedSegmentException("missing required attribute x1", location)
    val sy1 = y1 ?: throw MalformedSegmentException("missing required attribute y1", location)
    val sx2 = x2 ?: throw MalformedSegmentException("missing required attribute x2", location)
    val sy2 = y2 ?: throw MalformedSegmentException("missing required attribute y2", location)

    val (cx1, cy1) = svgToCga(sx1, sy1, viewBox)
    val (cx2, cy2) = svgToCga(sx2, sy2, viewBox)

    val segment = try {
        Segment(FlatPoint(cx1, cy1), FlatPoint(cx2, cy2))
    } catch (_: DegenerateDipoleException) {
        throw MalformedSegmentException("coincident endpoints", location)
    }

    return SegmentNode(segment, SvgDefaults.strokedStyle(stroke, strokeWidth, strokeCap), tags)
}
