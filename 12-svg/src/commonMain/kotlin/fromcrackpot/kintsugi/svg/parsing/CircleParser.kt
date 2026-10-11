package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_AW
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KFLAG_IMAGINARY
import fromcrackpot.kintsugi.svg.KFLAG_INSIDE_OUT
import fromcrackpot.kintsugi.svg.KTYPE_CIRCLE
import fromcrackpot.kintsugi.svg.KTYPE_ROUND_POINT
import fromcrackpot.kintsugi.svg.MalformedCircleException
import fromcrackpot.kintsugi.svg.MalformedRoundPointException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kAttrAsDouble
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.parseKFlags
import fromcrackpot.kintsugi.svg.svgToCga
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

private val KNOWN_CIRCLE_FLAGS = setOf(KFLAG_IMAGINARY, KFLAG_INSIDE_OUT)

/**
 * Parses a `<circle>` element (cursor on its START_ELEMENT). The `k:type` fork:
 *
 * - `k:type="round-point"` → [RoundPoint] (the tagged reading, via [parseRoundPointCircle]).
 * - untagged (or unknown `k:type`, which warns) → [Circle] — the untagged default, so
 *   designer-authored circles "just work." Discrimination is strictly by `k:type`, never by `r`.
 */
internal fun XmlReader.parseCircleElement(viewBox: SceneBounds, inherited: InheritedStyle): ShapeNode {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)
    val tags = inherited.tags + kTags()

    if (kType == KTYPE_ROUND_POINT) {
        return parseRoundPointCircle(viewBox, inherited.fill, tags)
    }
    if (kType != null && kType != KTYPE_CIRCLE) {
        warn("unknown k:type=\"$kType\" on <circle>${loc?.let { " at $it" } ?: ""}; treating as a circle")
    }
    return circleFrom(
        cx = attrAsDouble("cx"),
        cy = attrAsDouble("cy"),
        r = attrAsDouble("r"),
        flags = kAttr(KATTR_FLAGS),
        fill = readColorAttr("fill", loc) ?: inherited.fill,
        stroke = readColorAttr("stroke", loc) ?: inherited.stroke,
        strokeWidth = attrAsDouble("stroke-width") ?: inherited.strokeWidth,
        tags = tags, viewBox = viewBox, location = loc,
    )
}

/**
 * Parses a `<circle k:type="round-point">` (cursor on its START_ELEMENT) into a
 * `RoundPointNode`. Also invoked by `DipoleParser` for the dipole carrier's two
 * round-point children — the caller has already checked the `k:type` discriminator.
 *
 * The element `r` is the render marker size (`size = 2·r`), NOT the algebraic radius. The point's
 * raw `aw` (e4 blade weight) rides in `k:aw`; the serializer always emits it, but the parser
 * remains tolerant of its absence for hand-authored input — falling back to the 2-arg conformal
 * lift (`RoundPoint(cgaX, cgaY)`), which by construction lands on the null cone
 * (`aw = (ax² + ay²) / 2 ⇒ radius² ≡ 0`). When `k:aw` is present, it is fed straight into the
 * 4-arg ctor — no recombination — so an imaginary round point (`radius² < 0`, encoded as a small
 * `k:aw`) round-trips at the `Double` level.
 *
 * `aw` is a CGA-space blade weight, consistent with the CGA-space position the parser reconstructs
 * from `cx`/`cy`; it is independent of the SVG render marker `r`.
 */
internal fun XmlReader.parseRoundPointCircle(
    viewBox: SceneBounds,
    fallbackFill: ShapeColor?,
    tags: Set<String> = emptySet(),
): RoundPointNode {
    val loc = extLocationInfo?.toString()
    val cx = attrAsDouble("cx")
    val cy = attrAsDouble("cy")
    val r = attrAsDouble("r")
    val aw = kAttrAsDouble(KATTR_AW)
    val fill = readColorAttr("fill", loc) ?: fallbackFill

    val rv = r ?: throw MalformedRoundPointException("missing required attribute r", loc)
    if (rv <= 0.0) {
        throw MalformedRoundPointException("r must be > 0, was $rv", loc)
    }
    val (cgaX, cgaY) = svgToCga(cx ?: 0.0, cy ?: 0.0, viewBox)

    val point = if (aw != null) RoundPoint(cgaX, cgaY, az = 1.0, aw = aw) else RoundPoint(cgaX, cgaY)
    val style = ShapeStyle(
        fill = fill ?: SvgDefaults.FILL,
        size = 2.0 * rv,
    )
    return RoundPointNode(point, style, tags)
}

/**
 * Builds a `CircleNode` from the attributes of a plain `<circle>` (no `k:type`).
 *
 * `cx`/`cy` are the geometric center, `r = √|radius²|`. `k:flags` carries CGA semantics SVG can't
 * express: `imaginary` (`radius² < 0`) and `inside-out` (`cw > 0`). Both are reconstructed exactly
 * via the 4-arg [Circle] ctor (the friendly center/radius ctor only ever yields a real, classic
 * circle). Unknown flag tokens warn and are ignored.
 */
private fun circleFrom(
    cx: Double?,
    cy: Double?,
    r: Double?,
    flags: String?,
    fill: ShapeColor?,
    stroke: ShapeColor?,
    strokeWidth: Double?,
    tags: Set<String>,
    viewBox: SceneBounds,
    location: String? = null,
): CircleNode {
    val rv = r ?: throw MalformedCircleException("missing required attribute r", location)
    if (rv <= 0.0) {
        throw MalformedCircleException("r must be > 0, was $rv", location)
    }
    val sx = cx ?: 0.0
    val sy = cy ?: 0.0
    val (cgaX, cgaY) = svgToCga(sx, sy, viewBox)

    val tokens = parseKFlags(flags, KNOWN_CIRCLE_FLAGS, "circle", location)
    val imaginary = KFLAG_IMAGINARY in tokens
    val insideOut = KFLAG_INSIDE_OUT in tokens

    // Build the classic (cw = -1) circle at the geometric center with the signed radius², then take
    // its inside-out twin (negate every component — same point set, opposite orientation) if flagged.
    // Classic circle: cz = -½(cx² + cy² - radius²); center.ax = -cx·cw recovers the geometric center.
    val radius2 = if (imaginary) -(rv * rv) else (rv * rv)
    var ccx = cgaX
    var ccy = cgaY
    var ccz = -0.5 * (cgaX * cgaX + cgaY * cgaY - radius2)
    var ccw = -1.0
    if (insideOut) {
        ccx = -ccx; ccy = -ccy; ccz = -ccz; ccw = -ccw
    }
    val circle = Circle(ccx, ccy, ccz, ccw)

    return CircleNode(circle, SvgDefaults.style(fill, stroke, strokeWidth), tags)
}
