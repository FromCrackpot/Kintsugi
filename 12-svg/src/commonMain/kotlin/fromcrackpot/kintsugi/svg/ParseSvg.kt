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
 * The root `viewBox` becomes the scene's bounds, and every coordinate maps viewBox-relative into
 * CGA model space (centered, Y-up) at full `Double` precision — nothing is quantized on load;
 * canonical equality is the shapes' own rounded identity. Styles resolve here to a fully concrete
 * per-shape style: root-`<svg>` and `<g>` presentation attributes cascade nearest-wins into each
 * shape's unset slots, `k:tags` union down onto every leaf, and no inheritance escapes into the
 * scene.
 *
 * [templates] backs external `<use href="doc.svg">` expansion: each key is a referenced document's
 * path (its `href` up to any `#`, which is tolerated and ignored), each value that document's raw
 * SVG text. A `<use>` site re-parses its template inside the site's own inheritance chain, so
 * use-site presentation attributes and `k:tags` flow into the template's unset slots — a
 * per-instance restyle of the shared template. Documents without `<use>` need no templates;
 * [scanSvgRefs] lists the paths a document references.
 *
 * @param content The SVG document text.
 * @param templates Referenced document path → that document's raw SVG text; empty by default.
 * @return The scene: the parsed children in source order, bounds carrying the viewBox.
 * @throws MissingTemplateException when a `<use>` references a document absent from [templates].
 * @throws SvgFormatException when the document violates the `/v1` format — every failure is a
 *   subtype naming its condition.
 * @throws XmlException when the text is not well-formed XML — below the format layer, so outside
 *   the [SvgFormatException] hierarchy.
 */
public fun parseSvg(content: String, templates: Map<String, String> = emptyMap()): GroupNode =
    parseDocument(content, ParseContext(templates), InheritedStyle(), templateKey = null)

/**
 * Returns the paths of the external documents [content] references through `<use>`.
 *
 * One cheap pull-parse pass collecting every `<use>` element's `href` up to any `#` — the fetch
 * pass for [parseSvg]'s templates map: scan the layout, fetch each referenced document, re-scan
 * those for transitive references, then parse once with the full map. Nothing is validated here;
 * a malformed `<use>` is the parse's diagnosis to make.
 *
 * @param content The SVG document text.
 * @return The referenced document paths, deduplicated; empty when the document has no `<use>`.
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
 * Shared document entry for the public API and `<use>` template expansion. [outerInherited] seeds
 * the root's inheritance chain — empty for a top-level parse, the use-site's chain for a template
 * (so use-site presentation attrs and `k:tags` flow into the template's unset slots). A non-null
 * [templateKey] marks a template parse: the root must satisfy the template size contract
 * (explicit `width`/`height` equal to the viewBox extents — see [validateTemplateSize]).
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
            // Root <svg> presentation attrs seed the inheritance chain — own attr → ancestor-<g>
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
