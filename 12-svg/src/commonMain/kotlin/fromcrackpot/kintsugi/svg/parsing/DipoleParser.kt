package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_ROUND_POINT
import fromcrackpot.kintsugi.svg.MalformedDipoleException
import fromcrackpot.kintsugi.svg.decodeDipole
import fromcrackpot.kintsugi.svg.kAttr
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Reads the children of a `<g k:type="dipole">` (already entered via START_ELEMENT) up to the
 * matching END_ELEMENT.
 *
 * - Collects exactly two `<circle k:type="round-point">` children and silently ignores the dashed
 *   `<line>` chrome.
 * - Child order is the orientation contract: the first round point is the left operand of
 *   `start ∧ end`. [dipoleFrom] wedges them and turns coincident endpoints into [MalformedDipoleException].
 * - The `DipoleNode`'s style comes from the first child. Mismatched children silently take the
 *   first's. `ShapeStyle.size` rides on the child `<circle>`'s `r`, as for round points.
 */
internal fun XmlReader.parseDipoleGroup(
    viewBox: SceneBounds,
    fill: ShapeColor?,
    tags: Set<String>,
    groupLocation: String?,
): DipoleNode {
    val rps = mutableListOf<RoundPointNode>()
    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> {
                val loc = extLocationInfo?.toString()
                when (localName) {
                    "circle" -> {
                        if (kAttr(KATTR_TYPE) != KTYPE_ROUND_POINT) {
                            throw MalformedDipoleException(
                                "child <circle> must carry k:type=\"round-point\"", loc,
                            )
                        }
                        if (rps.size >= 2) {
                            throw MalformedDipoleException(
                                "expected exactly 2 round-point children, found more", loc,
                            )
                        }
                        rps += parseRoundPointCircle(viewBox, fill)
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
                    if (rps.size != 2) {
                        throw MalformedDipoleException(
                            "expected exactly 2 round-point children, found ${rps.size}",
                            groupLocation,
                        )
                    }
                                        return dipoleFrom(rps[0], rps[1], rps[0].style, tags, groupLocation)
                }
            }
            else -> Unit
        }
    }
    throw MalformedDipoleException("missing </g>", groupLocation)
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
 *   `MalformedDipoleException` here. A Plücker violation is impossible: the wedge of two
 *   factorizable round points is a factorizable blade.
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
