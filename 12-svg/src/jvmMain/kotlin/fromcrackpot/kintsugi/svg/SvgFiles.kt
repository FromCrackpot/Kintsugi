package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import java.io.File
import java.io.IOException
import nl.adaptivity.xmlutil.XmlException

/**
 * Parses this file's UTF-8 SVG text into a [GroupNode] scene.
 *
 * - No templates map, so a `<use>` throws [MissingTemplateException]. Use the string form with templates.
 *
 * @receiver The SVG file to read.
 * @return The scene: the parsed children in source order, bounds carrying the viewBox.
 * @throws IOException when the file cannot be read.
 * @throws SvgFormatException when the document violates the `/v1` format. Each failure is a
 *   subtype naming its condition.
 * @throws XmlException when the text is not well-formed XML. It sits outside the
 *   [SvgFormatException] hierarchy.
 */
public fun File.parseSvg(): GroupNode = parseSvg(readText(Charsets.UTF_8))

/**
 * Writes this scene to [file] as UTF-8 SVG text in the module's `/v1` format, creating or
 * truncating it.
 *
 * @param file The destination file.
 * @throws MissingViewBoxException when this scene's bounds is `null`.
 * @throws IOException when the file cannot be written.
 */
public fun GroupNode.writeTo(file: File) {
    file.writeText(serializeSvg(this), Charsets.UTF_8)
}
