package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.svg.UnsupportedSvgElementException
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Reads the children of a group element (`<svg>` root or nested `<g>`) until its matching
 * END_ELEMENT, dispatching each child element to its parser. Caller must have already consumed the
 * group's START_ELEMENT and read its presentational attributes into the [inherited] chain.
 *
 * Each arm is a one-liner: the element's parser owns everything about the element — attribute
 * reads, the `k:type` fork (tagged reading vs untagged default), validation, construction.
 * Element identity is resolved by `k:type`, never by geometry. Any element outside the supported
 * set (`<rect>`, `<circle>`, `<line>`, `<polygon>`, `<polyline>`, `<g>`, `<use>`) raises
 * [UnsupportedSvgElementException].
 */
internal fun XmlReader.parseGroupChildren(
    groupTagName: String,
    viewBox: SceneBounds,
    inherited: InheritedStyle,
    ctx: ParseContext,
): List<Node> {
    val children = mutableListOf<Node>()

    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> {
                children += when (localName) {
                    "rect" -> parseRectElement(viewBox, inherited)
                    "circle" -> parseCircleElement(viewBox, inherited)
                    "line" -> parseLineElement(viewBox, inherited)
                    "polygon" -> parsePolygonElement(viewBox, inherited)
                    "polyline" -> parsePolylineElement(viewBox, inherited)
                    "g" -> parseGElement(viewBox, inherited, ctx)
                    "use" -> parseUseElement(viewBox, inherited, ctx)
                    else -> throw UnsupportedSvgElementException(localName, extLocationInfo?.toString())
                }
            }
            EventType.END_ELEMENT -> {
                if (localName == groupTagName) return children
            }
            EventType.IGNORABLE_WHITESPACE,
            EventType.TEXT,
            EventType.COMMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.START_DOCUMENT,
            EventType.END_DOCUMENT,
            EventType.DOCDECL,
            EventType.ENTITY_REF,
            EventType.CDSECT,
            EventType.ATTRIBUTE -> Unit
        }
    }
    // Reached only if the document ends without the matching END_ELEMENT — malformed XML, but
    // xmlutil's pull parser would already have thrown; reaching here means an empty document or
    // a root element with no opening tag. Returning current state matches the prior parser's
    // permissive behavior.
    return children
}
