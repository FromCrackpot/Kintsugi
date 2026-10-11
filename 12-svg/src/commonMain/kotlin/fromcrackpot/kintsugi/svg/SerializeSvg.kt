package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.worldBorder
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.writeNodes
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion

/**
 * Serializes [root] into SVG text in the module's `/v1` format.
 *
 * The root's [GroupNode.bounds] becomes the `viewBox` and is required; a nested group's non-null
 * bounds cannot be encoded and is dropped with a warning. Numbers are written at full stored
 * `Double` precision, never quantized — the file keeps the raw components, and canonical equality
 * is rebuilt on load by the shapes' rounded identity — so `parseSvg(serializeSvg(scene)) == scene`.
 * Output is canonical: fixed attribute order, lowercase hex colors, shortest round-trippable
 * numbers. Canonical per platform, though — `Double.toString` differs between targets — so never
 * compare serialized text across platforms.
 *
 * Degenerate CGA states persist faithfully: imaginary and inside-out circles ride `k:flags`, and
 * an infinite line writes its world-border crossings as display endpoints with recognition riding
 * `k:type` — data loss is never the persistence layer's call. The exception is a shape the format
 * cannot place at all: a line at infinity, or a dipole whose endpoints lie at infinity, is skipped
 * with a warning.
 *
 * @param root The scene to write; its bounds must be non-null.
 * @return The complete SVG document text.
 * @throws MissingViewBoxException when [root]'s bounds is `null`.
 */
public fun serializeSvg(root: GroupNode): String {
    val viewBox = root.bounds ?: throw MissingViewBoxException()
    val worldBorder = viewBox.worldBorder()
    val out = StringBuilder()
    val writer = KtXmlWriter(
        writer = out,
        isRepairNamespaces = false,
        xmlDeclMode = XmlDeclMode.Minimal,
        xmlVersion = XmlVersion.XML10,
    )
    writer.indentString = "  "
    writer.addTrailingSpaceBeforeEnd = false
    try {
        writer.startTag(SVG_NS, "svg", "")
        writer.namespaceAttr("", SVG_NS)
        writer.namespaceAttr(KINTSUGI_PREFIX, KINTSUGI_NS)
        writer.attribute(null, "viewBox", null, viewBox.toAttributeValue())

        writer.writeNodes(root.children, viewBox, worldBorder)

        writer.endTag(SVG_NS, "svg", "")
        writer.endDocument()
    } finally {
        writer.close()
    }
    return out.toString()
}

private fun SceneBounds.toAttributeValue(): String =
    "${minX.formatSvgNumber()} ${minY.formatSvgNumber()} ${width.formatSvgNumber()} ${height.formatSvgNumber()}"
