package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.svg.parsing.InheritedStyle
import fromcrackpot.kintsugi.svg.parsing.ParseContext
import fromcrackpot.kintsugi.svg.parsing.parseGroupChildren
import fromcrackpot.kintsugi.svg.parsing.readInheritedStyle
import fromcrackpot.kintsugi.svg.parsing.readViewBox
import fromcrackpot.kintsugi.svg.parsing.validateTemplateSize
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.xmlStreaming

/**
 * Parses [content], an SVG document in the module's `/v1` format, into a [GroupNode] scene.
 *
 * - Styles resolve to concrete per-leaf styles: presentation attributes cascade, `k:tags` union down.
 * - External `<use href>` documents come from [templates]; [scanSvgRefs] lists the paths to fetch.
 *
 * @param content The SVG document text.
 * @param templates Referenced document path → that document's raw SVG text, empty by default.
 * @return The scene: the parsed children in source order, bounds carrying the viewBox.
 * @throws MissingTemplateException when a `<use>` references a document absent from [templates].
 * @throws SvgFormatException when the document violates the `/v1` format. Each failure is a
 *   subtype naming its condition.
 * @throws XmlException when the text is not well-formed XML. It sits outside the
 *   [SvgFormatException] hierarchy.
 */
public fun parseSvg(content: String, templates: Map<String, String> = emptyMap()): GroupNode =
    parseDocument(content, ParseContext(templates), InheritedStyle(), templateKey = null)

/**
 * Returns the paths of the external documents [content] references through `<use>`.
 *
 * - Fetch them, re-scan those for their own references, then parse once with the full map.
 * - Validates nothing: a malformed `<use>` is the parse's to report.
 *
 * @param content The SVG document text.
 * @return The referenced document paths, deduplicated. Empty when the document has no `<use>`.
 * @throws XmlException when the text is not well-formed XML.
 */
public fun scanSvgRefs(content: String): Set<String> {
    val reader = xmlStreaming.newGenericReader(content)
    val refs = mutableSetOf<String>()
    try {
        while (reader.hasNext()) {
            if (reader.next() != EventType.START_ELEMENT) continue
            if (reader.localName != "use") continue
            val doc = reader.getAttributeValue(null, "href")?.substringBefore('#')
            if (!doc.isNullOrEmpty()) refs += doc
        }
    } finally {
        reader.close()
    }
    return refs
}

/**
 * Shared document entry for the public API and `<use>` template expansion.
 * - [outerInherited] seeds the root's inheritance chain: empty for a top-level parse, the use-site's
 *   chain for a template. Use-site presentation attrs and `k:tags` then fill the template's unset slots.
 * - A non-null [templateKey] marks a template parse. The root must then satisfy the template size
 *   contract: explicit `width`/`height` equal to the viewBox extents (see [validateTemplateSize]).
 */
internal fun parseDocument(
    content: String,
    ctx: ParseContext,
    outerInherited: InheritedStyle,
    templateKey: String?,
): GroupNode {
    val reader = xmlStreaming.newGenericReader(content)
    try {
        while (reader.hasNext()) {
            if (reader.next() != EventType.START_ELEMENT) continue
            val loc = reader.extLocationInfo?.toString()
            if (reader.localName != "svg") throw UnsupportedSvgElementException(reader.localName, loc)
            val viewBox = reader.readViewBox()
            if (templateKey != null) reader.validateTemplateSize(viewBox, templateKey, loc)
            reader.warnUnknownChrome(loc)
            // Root <svg> presentation attrs seed the inheritance chain: own attr → ancestor-<g>
            // chain → root attr → SvgDefaults. Per-attribute fallback layer above the SVG 1.1 defaults.
            val rootInherited = outerInherited.child(reader.readInheritedStyle(loc))
            val children = reader.parseGroupChildren(
                groupTagName = "svg",
                viewBox = viewBox,
                inherited = rootInherited,
                ctx = ctx,
            )
            return GroupNode(children = children, bounds = viewBox)
        }
        throw MissingSvgRootException()
    } finally {
        reader.close()
    }
}
