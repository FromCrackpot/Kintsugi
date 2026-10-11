package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import java.io.File
import java.io.IOException
import nl.adaptivity.xmlutil.XmlException

/**
 * Parses this file's UTF-8 SVG text into a [GroupNode] scene.
 *
 * The string entry point `parseSvg(content)` with the file read inline. There is no templates
 * map, so the document must be self-contained — a `<use>` reference throws
 * [MissingTemplateException]; fetch the templates and call the string form directly when a
 * layout needs them.
 *
 * @receiver The SVG file to read.
 * @return The scene: the parsed children in source order, bounds carrying the viewBox.
 * @throws IOException when the file cannot be read.
 * @throws SvgFormatException when the document violates the `/v1` format — every failure is a
 *   subtype naming its condition.
 * @throws XmlException when the text is not well-formed XML — below the format layer, so outside
 *   the [SvgFormatException] hierarchy.
 */
public fun File.parseSvg(): GroupNode = parseSvg(readText(Charsets.UTF_8))

/**
 * Writes this scene to [file] as UTF-8 SVG text in the module's `/v1` format.
 *
 * The string entry point `serializeSvg(root)` with the write inline; the file is created or
 * truncated.
 *
 * @param file The destination file.
 * @throws MissingViewBoxException when this scene's bounds is `null`.
 * @throws IOException when the file cannot be written.
 */
public fun GroupNode.writeTo(file: File) {
    file.writeText(serializeSvg(this), Charsets.UTF_8)
}
