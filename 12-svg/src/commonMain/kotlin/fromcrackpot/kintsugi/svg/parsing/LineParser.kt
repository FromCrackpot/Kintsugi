package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KFLAG_IMAGINARY
import fromcrackpot.kintsugi.svg.KTYPE_LINE
import fromcrackpot.kintsugi.svg.MalformedLineException
import fromcrackpot.kintsugi.svg.MalformedSegmentException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.decodeSegment
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.parseKFlags
import fromcrackpot.kintsugi.svg.presentationAsDouble
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/**
 * Parses a `<line>` element (cursor on its START_ELEMENT). The `k:type` fork:
 *
 * - `k:type="line"` → infinite CGA [Line]. Recognition is by discriminator, never by endpoint
 *   geometry.
 * - untagged (or unknown `k:type`, which warns) → [Segment], the untagged default.
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
    val strokeWidth = presentationAsDouble("stroke-width") ?: inherited.strokeWidth
    val strokeCap = readStrokeCapAttr("stroke-linecap", loc) ?: inherited.strokeCap
    val style = SvgDefaults.strokedStyle(stroke, strokeWidth, strokeCap, readDash(inherited, loc))
    val tags = inherited.tags + kTags()

    if (kType == KTYPE_LINE) {
        return lineFrom(x1, y1, x2, y2, style, tags, viewBox, loc)
    }
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <line>${loc?.let { " at $it" } ?: ""}; treating as a segment")
    }
    return segmentFrom(x1, y1, x2, y2, kAttr(KATTR_FLAGS), style, tags, viewBox, loc)
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
    style: ShapeStyle,
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

    return LineNode(start join end, style, tags)
}

/**
 * Builds a `SegmentNode` from the attributes of a bare `<line>` (no `k:type`) through
 * [decodeSegment], as `Segment(FlatPoint(x1,y1), FlatPoint(x2,y2))`. Endpoint order encodes the
 * segment's orientation (same `start ∧ end` contract as Line, applied at the round-point level).
 *
 * `k:flags="imaginary"` marks an imaginary segment: the endpoints are its real-valued pair, and the
 * decoded real segment is swapped for its imaginary twin (same carrier and center, radius²
 * negated). Unknown flag tokens warn and are ignored. Endpoints coinciding at the `SCALE` quantum
 * throw `MalformedSegmentException` (matches the dipole encoding's posture).
 */
private fun segmentFrom(
    x1: Double?,
    y1: Double?,
    x2: Double?,
    y2: Double?,
    flags: String?,
    style: ShapeStyle,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): SegmentNode {
    val sx1 = x1 ?: throw MalformedSegmentException("missing required attribute x1", location)
    val sy1 = y1 ?: throw MalformedSegmentException("missing required attribute y1", location)
    val sx2 = x2 ?: throw MalformedSegmentException("missing required attribute x2", location)
    val sy2 = y2 ?: throw MalformedSegmentException("missing required attribute y2", location)

    val real = decodeSegment(sx1, sy1, sx2, sy2, viewBox)
        ?: throw MalformedSegmentException("coincident endpoints", location)
    val imaginary = KFLAG_IMAGINARY in parseKFlags(flags, KNOWN_SEGMENT_FLAGS, "line", location)
    val segment = if (imaginary) real.imaginaryTwin() else real

    return SegmentNode(segment, style, tags)
}

private val KNOWN_SEGMENT_FLAGS = setOf(KFLAG_IMAGINARY)

// Same carrier and center, radius² ↦ −radius²: (px, py) += radius²·(−gy, gx) on the unit carrier normal.
private fun Segment.imaginaryTwin(): Segment =
    Dipole(gx, gy, gz, px - gy * radius2, py + gx * radius2, pz).toSegment
