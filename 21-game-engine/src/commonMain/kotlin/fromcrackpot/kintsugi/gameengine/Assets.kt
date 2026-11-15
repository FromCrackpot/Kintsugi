package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.bfxr.BfxrFormatException
import fromcrackpot.kintsugi.bfxr.parseBfxr
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.svg.SvgFormatException
import fromcrackpot.kintsugi.svg.SvgTemplateCycleException
import fromcrackpot.kintsugi.svg.parseSvg
import fromcrackpot.kintsugi.svg.scanSvgRefs

/**
 * The load entry: SVG assets arrive through it as ready scene-graph values, `.bfxr` files as rendered sounds.
 *
 * - Every load reads through the scope's reader: `Res::readBytes` in a game, file reads or a map headless.
 * - Assets arrive viewBox-centered on the CGA origin; place them with `node * versor`.
 *
 * @constructor Builds the scope over the byte reader every load reads through.
 */
public class AssetScope(
    private val resources: suspend (String) -> ByteArray,
) {
    /**
     * Loads the SVG document at [path] as a scene sub-tree.
     *
     * - `<use href>` templates resolve transparently, read as sibling files of the referencing document.
     * - Text that is not well-formed XML fails with the reader's `XmlException`.
     *
     * @param path The document's resource path.
     * @return The document as a group: children in source order, bounds carrying the viewBox.
     * @throws SvgFormatException when a document violates the `/v1` format. Each failure is a
     *   subtype naming its condition, a template cycle included.
     */
    public suspend fun loadGroup(path: String): GroupNode {
        val text = resources(path).decodeToString()
        val refs = scanSvgRefs(text)
        if (refs.isEmpty()) return parseSvg(text)

        // Every href scanSvgRefs reports is fetched below, so MissingTemplateException is
        // unreachable from here. Both sides read the href up to any '#'.
        val templates = mutableMapOf<String, String>()
        val pending = ArrayDeque(refs)
        while (pending.isNotEmpty()) {
            val href = pending.removeFirst()
            if (href in templates) continue
            val templateText = resources(siblingPath(path, href)).decodeToString()
            templates[href] = templateText
            scanSvgRefs(templateText).forEach { if (it !in templates) pending.add(it) }
        }
        return parseSvg(text, templates)
    }

    /**
     * Loads the single-shape SVG document at [path] as its typed leaf:
     * `load<PolygonNode>("files/sprite.svg")` yields a `PolygonNode`.
     *
     * @param N The expected concrete leaf type.
     * @param path The document's resource path.
     * @return The document's one leaf as [N].
     * @throws NoSuchElementException when the document holds no shape at all.
     * @throws IllegalArgumentException when the document holds more than one shape, or its leaf is
     *   not an [N].
     * @throws SvgFormatException when a document violates the `/v1` format. Each failure is a
     *   subtype naming its condition.
     */
    public suspend inline fun <reified N : ShapeNode> load(path: String): N {
        val node = singleShape(path)
        require(node is N) {
            "asset $path contains ${node::class.simpleName}, expected ${N::class.simpleName}"
        }
        return node
    }

    /**
     * Loads every shape of the SVG document at [path] as typed leaves, in document order:
     * `loadAll<PolygonNode>("files/tiles.svg")` yields a `List<PolygonNode>`.
     *
     * - Any shape count is accepted. A shapeless document yields an empty list.
     *
     * @param N The expected concrete leaf type of every shape.
     * @param path The document's resource path.
     * @return The document's leaves as [N]s, in document order.
     * @throws IllegalArgumentException when any leaf in the document is not an [N].
     * @throws SvgFormatException when a document violates the `/v1` format. Each failure is a
     *   subtype naming its condition.
     */
    public suspend inline fun <reified N : ShapeNode> loadAll(path: String): List<N> =
        allShapes(path).map { node ->
            require(node is N) {
                "asset $path contains ${node::class.simpleName}, expected ${N::class.simpleName}"
            }
            node
        }

    /**
     * Loads the `.bfxr` sound at [path], rendered and ready to play.
     *
     * @param path The file's resource path.
     * @return The rendered sound.
     * @throws BfxrFormatException when the file isn't a valid `.bfxr` sound. It lists every problem at once.
     */
    public suspend fun loadSound(path: String): Sound = Sound(parseBfxr(resources(path).decodeToString()).render())

    @PublishedApi
    internal suspend fun singleShape(path: String): ShapeNode =
        allShapes(path).single()

    @PublishedApi
    internal suspend fun allShapes(path: String): List<ShapeNode> =
        loadGroup(path).flattenShapes()
}

/**
 * Resolves a `<use>` href against the referencing document's directory. Hrefs are plain
 * sibling file names, with deliberately no `..`/absolute handling.
 */
internal fun siblingPath(documentPath: String, href: String): String {
    val dir = documentPath.substringBeforeLast('/', "")
    return if (dir.isEmpty()) href else "$dir/$href"
}
