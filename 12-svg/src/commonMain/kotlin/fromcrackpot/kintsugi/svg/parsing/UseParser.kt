package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.times
import fromcrackpot.kintsugi.svg.MalformedTemplateException
import fromcrackpot.kintsugi.svg.MalformedUseException
import fromcrackpot.kintsugi.svg.MissingTemplateException
import fromcrackpot.kintsugi.svg.SvgTemplateCycleException
import fromcrackpot.kintsugi.svg.attrAsDouble
import fromcrackpot.kintsugi.svg.parseDocument
import fromcrackpot.kintsugi.svg.svgToCga
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Parses a `<use href="doc.svg" x=".." y=".."/>` element (cursor on its START_ELEMENT) by
 * **expanding** the referenced template document in place — parse-only, the `<rect>` precedent:
 * the expansion serializes back as plain placed shapes, never as a `<use>`.
 *
 * Reference semantics (external whole-document, the `/v1` contract):
 * - `href` is **required** and names a whole document in [ParseContext.templates]; a `#fragment`
 *   suffix is tolerated and ignored (the path before `#` keys the map). A bare-fragment href
 *   (`#id` — internal reference) is unsupported and malformed.
 * - **Origin anchoring**, matching browser semantics for a referenced `<svg>` root: the template's
 *   viewport top-left lands at `(x, y)` in this document's coordinates. The template parses
 *   viewBox-centered on the CGA origin, so expansion is one translation — the template's center
 *   point `(x + w/2, y + h/2)` mapped through the Y-flip, applied as `* T(...)` over the
 *   template's children.
 * - **No scaling ever**: `width`/`height` on `<use>` are rejected — one CGA unit is one SVG user
 *   unit in every document (root tenet), so a bigger brick is a different template.
 * - Use-site presentation attrs and `k:tags` seed the template's inheritance chain (SVG's cascade
 *   into the use shadow tree): the template's *unset* slots inherit per-instance fill/stroke/tags,
 *   explicit template attrs win. Tags union onto every expanded leaf.
 * - Transitive templates are legal (a template may `<use>` another, resolved from the same map);
 *   a closed loop throws [SvgTemplateCycleException] naming the chain.
 *
 * The expansion returns one [GroupNode] wrapping the template's children (bounds `null` — only
 * the root document's bounds round-trip), so a multi-shape template splices as a unit.
 */
internal fun XmlReader.parseUseElement(
    viewBox: SceneBounds,
    inherited: InheritedStyle,
    ctx: ParseContext,
): Node {
    val loc = extLocationInfo?.toString()

    val href = getAttributeValue(null, "href")
        ?: throw MalformedUseException("missing required attribute href", loc)
    val templateKey = href.substringBefore('#')
    if (templateKey.isEmpty()) {
        throw MalformedUseException(
            "href \"$href\" has no document path (internal #id references are not supported)", loc,
        )
    }
    if (attrAsDouble("width") != null || attrAsDouble("height") != null) {
        throw MalformedUseException(
            "width/height not supported (no scaling — one CGA unit is one SVG unit in every document)", loc,
        )
    }
    val x = attrAsDouble("x") ?: 0.0
    val y = attrAsDouble("y") ?: 0.0
    // Use-site fill/stroke/… and k:tags flow into the template's unset slots.
    val childInherited = inherited.child(readInheritedStyle(loc))
    skipToUseEnd(loc)

    if (templateKey in ctx.expansionStack) {
        throw SvgTemplateCycleException(ctx.expansionStack + templateKey)
    }
    val text = ctx.templates[templateKey] ?: throw MissingTemplateException(templateKey, loc)
    val template = parseDocument(text, ctx.push(templateKey), childInherited, templateKey)

    // Parser-produced root bounds are never null; the template's are additionally size-validated.
    val bounds = template.bounds!!
    val (tx, ty) = svgToCga(x + bounds.width / 2.0, y + bounds.height / 2.0, viewBox)
    return GroupNode(children = template.children) * T(tx, ty)
}

/** Consume events to the matching `</use>`; any child element is malformed (a `<use>` is empty). */
private fun XmlReader.skipToUseEnd(useLocation: String?) {
    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> throw MalformedUseException(
                "must be empty, found child <$localName>", extLocationInfo?.toString(),
            )
            EventType.END_ELEMENT -> if (localName == "use") return
            else -> Unit
        }
    }
    throw MalformedUseException("missing </use>", useLocation)
}

/**
 * The template size contract, validated on a template's root `<svg>` (cursor on it): explicit
 * `width`/`height` equal to the viewBox extents. Required for browser fidelity — a sizeless
 * nested `<svg>` defaults to 100% and inflates to the whole viewport in standard renderers, so
 * the load boundary turns that silent blowup into a diagnosis. (A template with `width`/`height`
 * and no `viewBox` passes trivially: the viewBox fallback derives from those extents.)
 */
internal fun XmlReader.validateTemplateSize(viewBox: SceneBounds, templateKey: String, location: String?) {
    val w = attrAsDouble("width")
    val h = attrAsDouble("height")
    if (w == null || h == null) {
        throw MalformedTemplateException(
            "\"$templateKey\" must declare explicit width/height on its root <svg> " +
                "(a sizeless nested svg renders at 100% in browsers)",
            location,
        )
    }
    if (w != viewBox.width || h != viewBox.height) {
        throw MalformedTemplateException(
            "\"$templateKey\" width/height ($w x $h) must equal its viewBox extents " +
                "(${viewBox.width} x ${viewBox.height})",
            location,
        )
    }
}
