package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_ATTITUDE
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_ROUND_POINT
import fromcrackpot.kintsugi.svg.MalformedDipoleException
import fromcrackpot.kintsugi.svg.decodeDipole
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.svgToCgaDirection
import fromcrackpot.kintsugi.svg.warnUnknownChrome
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

private val ATTITUDE_SEPARATORS = Regex("[\\s,]+")

/**
 * Reads the children of a `<g k:type="dipole">` (already entered via START_ELEMENT) up to the
 * matching END_ELEMENT.
 *
 * - Endpoint form, no `k:attitude`: exactly two `<circle k:type="round-point">` children. Child
 *   order is the orientation contract: the first is the left operand of `start ∧ end` ([dipoleFrom]).
 * - Center form, `k:attitude` on the carrier: exactly one round-point child, the center
 *   ([centeredDipoleFrom]).
 * - Both forms silently ignore the dashed `<line>` chrome.
 * - The `DipoleNode`'s style comes from the first child. Mismatched children silently take the
 *   first's. `ShapeStyle.size` rides on the child `<circle>`'s `r`, as for round points.
 * - [carrier] is the carrier `<g>`'s own attrs over the inherited chain: the children's fallback
 *   `fill` and dash (the writer puts an authored dash on the carrier), and the dipole's tags.
 */
internal fun XmlReader.parseDipoleGroup(
    viewBox: SceneBounds,
    carrier: InheritedStyle,
    groupLocation: String?,
): DipoleNode {
    val attitude = readAttitude(groupLocation)
    val expected = if (attitude == null) 2 else 1
    val rps = mutableListOf<RoundPointNode>()
    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> {
                val loc = extLocationInfo?.toString()
                warnUnknownChrome(loc)
                when (localName) {
                    "circle" -> {
                        if (kAttr(KATTR_TYPE) != KTYPE_ROUND_POINT) {
                            throw MalformedDipoleException(
                                "child <circle> must carry k:type=\"round-point\"", loc,
                            )
                        }
                        if (rps.size >= expected) {
                            throw MalformedDipoleException("${expectedChildren(expected)}, found more", loc)
                        }
                        rps += parseRoundPointCircle(viewBox, carrier)
                    }
                    // Visual chrome: the parser ignores the dashed <line> entirely, per spec.
                    "line" -> Unit
                    else -> throw MalformedDipoleException(
                        "unsupported child element <$localName>", loc,
                    )
                }
            }
            EventType.END_ELEMENT -> {
                if (localName == "g") {
                    if (rps.size != expected) {
                        throw MalformedDipoleException(
                            "${expectedChildren(expected)}, found ${rps.size}", groupLocation,
                        )
                    }
                    return if (attitude == null) {
                        dipoleFrom(rps[0], rps[1], rps[0].style, carrier.tags, groupLocation)
                    } else {
                        centeredDipoleFrom(rps[0], attitude, rps[0].style, carrier.tags, groupLocation)
                    }
                }
            }
            else -> Unit
        }
    }
    throw MalformedDipoleException("missing </g>", groupLocation)
}

private fun expectedChildren(expected: Int): String =
    if (expected == 1) "expected exactly 1 round-point child with k:attitude"
    else "expected exactly 2 round-point children"

/**
 * The carrier's `k:attitude` as a model-space [Direction], or `null` when absent.
 *
 * - Two finite numbers in the SVG frame, separated by whitespace and/or commas. Anything else
 *   throws `MalformedDipoleException`.
 */
private fun XmlReader.readAttitude(location: String?): Direction? {
    val raw = kAttr(KATTR_ATTITUDE) ?: return null
    val numbers = raw.trim().split(ATTITUDE_SEPARATORS).filter { it.isNotEmpty() }
        .map { it.toDoubleOrNull()?.takeIf(Double::isFinite) }
    if (numbers.size != 2 || numbers.any { it == null }) {
        throw MalformedDipoleException("k:attitude=\"$raw\" is not two numbers", location)
    }
    val (dx, dy) = svgToCgaDirection(numbers[0]!!, numbers[1]!!)
    return Direction(dx, dy)
}

/**
 * Rebuilds a `Dipole` from the two `RoundPoint` children of a `<g k:type="dipole">` as
 * `start join end`.
 *
 * - Child order is the orientation contract: swapping the operands flips the blade's sign.
 * - The Containers invariant `start ∧ end = D` makes the encoding canonical. Re-wedging the emitted
 *   `dipole.start` / `dipole.end` rebuilds the original blade, imaginary dipoles included
 *   (`sqrt(|radius²|)` preserves the sign of `gz²` through the wedge).
 * - Round points coinciding at the `SCALE` quantum ([decodeDipole] returns `null`) throw
 *   `MalformedDipoleException` here: a coincident pair carries no attitude, which the center form
 *   adds. A Plücker violation is impossible: the wedge of two factorizable round points is a
 *   factorizable blade.
 */
internal fun dipoleFrom(
    start: RoundPoint,
    end: RoundPoint,
    style: ShapeStyle,
    tags: Set<String> = emptySet(),
    location: String? = null,
): DipoleNode {
    val dipole = decodeDipole(start, end)
        ?: throw MalformedDipoleException("coincident round-point children", location)
    return DipoleNode(dipole, style, tags)
}

/**
 * Rebuilds a `Dipole` from the center form: [center]'s position, [attitude], and the signed
 * radius² that [center]'s `k:aw` carries.
 *
 * - Any radius² is accepted, real or imaginary. A save rewrites a non-zero one in the endpoint form.
 * - An [attitude] rounding to zero length at the `SCALE` quantum throws `MalformedDipoleException`.
 */
internal fun centeredDipoleFrom(
    center: RoundPoint,
    attitude: Direction,
    style: ShapeStyle,
    tags: Set<String> = emptySet(),
    location: String? = null,
): DipoleNode {
    val dipole = try {
        Dipole(center.toFlat, attitude, center.radius2)
    } catch (_: IllegalArgumentException) {
        throw MalformedDipoleException("k:attitude rounds to zero length", location)
    }
    return DipoleNode(dipole, style, tags)
}
