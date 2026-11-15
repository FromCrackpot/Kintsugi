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
 * Serializes [root] into SVG text in the module's `/v1` format, at full `Double` precision.
 *
 * - Round-trips exactly through `parseSvg`. The text is canonical per platform, so never compare it across targets.
 * - Dropped with a warning: a nested group's bounds, shapes at infinity, zero-length segments and dipoles.
 *
 * @param root The scene to write. Its bounds must be non-null.
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
