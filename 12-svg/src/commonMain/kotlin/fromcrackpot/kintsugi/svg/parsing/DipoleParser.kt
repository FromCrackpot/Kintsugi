package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_ROUND_POINT
import fromcrackpot.kintsugi.svg.MalformedDipoleException
import fromcrackpot.kintsugi.svg.kAttr
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Reads the children of a `<g k:type="dipole">` (already entered via START_ELEMENT) until the
 * matching END_ELEMENT, collecting exactly two `<circle k:type="round-point">` children and
 * silently ignoring the dashed `<line>` visual chrome. Child order is the orientation contract:
 * the first round-point becomes the left operand of `start ∧ end`. The two children are then
 * wedged via [dipoleFrom], which canonicalizes coincident endpoints into [MalformedDipoleException].
 *
 * Style for the resulting `DipoleNode` is taken from the first child (a designer authoring matched
 * fill/size on both children gets a consistent result; mismatched silently take the first's). The
 * `ShapeStyle.size` rides on the child `<circle>`'s `r` attribute, matching the round-point convention.
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
                    // Visual chrome — parser ignores the dashed <line> entirely per spec.
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
 * Reconstructs a `Dipole` from the two `RoundPoint` children of a `<g k:type="dipole">` via
 * `start join end`. Child order is the orientation contract: swapping the operands produces the
 * sign-flipped blade. The Containers invariant `start ∧ end = D` makes the encoding canonical —
 * emitting `dipole.start` / `dipole.end` (the lazy round-point properties) and re-wedging them
 * reconstructs the original blade, including for imaginary dipoles (`sqrt(|radius²|)` preserves
 * the sign of `gz²` through the wedge).
 *
 * Coincident endpoints collapse the carrier norm to zero — `:cga`'s `join` raises
 * `DegenerateDipoleException`, which we wrap as `MalformedDipoleException` at this boundary.
 * Plücker-violation is impossible here: the wedge of two factorizable round-points is a
 * factorizable blade by construction.
 */
internal fun dipoleFrom(
    start: RoundPoint,
    end: RoundPoint,
    style: ShapeStyle,
    tags: Set<String> = emptySet(),
    location: String? = null,
): DipoleNode {
    val dipole = try {
        start join end
    } catch (_: DegenerateDipoleException) {
        throw MalformedDipoleException("coincident round-point children", location)
    }
    return DipoleNode(dipole, style, tags)
}
