package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.svg.SvgFormatException
import fromcrackpot.kintsugi.svg.SvgTemplateCycleException
import fromcrackpot.kintsugi.svg.parseSvg
import fromcrackpot.kintsugi.svg.scanSvgRefs

/**
 * The load entry: SVG assets load through it, arriving as ready scene-graph values.
 *
 * Every load reads bytes through the reader the scope is built over. compose-resources
 * generates its `Res` accessor per application module, so a running game supplies
 * `Res::readBytes`; a headless session supplies plain file reads or an in-memory map and builds
 * its world the same way, `AssetScope(reader).run { … }`. A failure thrown by the reader
 * propagates unchanged. Assets arrive viewBox-centered on the CGA origin; world placement is
 * the caller's to apply — a loaded node moves like any other scene-graph value,
 * `node * versor`.
 *
 * @constructor Builds the scope over the byte reader every load reads through.
 */
public class AssetScope(
    private val resources: suspend (String) -> ByteArray,
) {
    /**
     * Loads the SVG document at [path] as a scene sub-tree.
     *
     * The document parses in the `/v1` SVG format via [parseSvg]. External `<use href="doc.svg">`
     * templates resolve transparently: the document is scanned for references, each referenced
     * document is fetched relative to this document's directory — hrefs are plain sibling file
     * names, so a document at `files/scene.svg` referencing `tile.svg` reads `files/tile.svg` —
     * fetched templates are re-scanned for transitive references, and the document parses once
     * with the full template map. A reference cycle fails the parse with
     * [SvgTemplateCycleException]; text that is not well-formed XML fails below the format layer
     * with the underlying reader's `XmlException`.
     *
     * @param path The document's resource path.
     * @return The document as a [GroupNode]: children in source order, bounds carrying the
     *   viewBox.
     * @throws SvgFormatException when a document violates the `/v1` format — every failure is a
     *   subtype naming its condition.
     */
    public suspend fun loadGroup(path: String): GroupNode {
        val text = resources(path).decodeToString()
        val refs = scanSvgRefs(text)
        if (refs.isEmpty()) return parseSvg(text)

        // Every href scanSvgRefs reports is fetched below, so MissingTemplateException is
        // unreachable from here — both sides read the href up to any '#'.
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
     * Loads the single-shape SVG document at [path] as its typed leaf —
     * `load<PolygonNode>("files/sprite.svg")` yields a `PolygonNode`.
     *
     * What a document contains is unknowable at compile time, so the one unavoidable runtime
     * assertion lives here, at the load boundary, with an error naming the found and expected
     * types — everything downstream works on the typed leaf cast-free, style and tags riding
     * along. The document loads exactly like [loadGroup], `<use>` templates included.
     *
     * @param N The expected concrete [ShapeNode] type.
     * @param path The document's resource path.
     * @return The document's one leaf as [N].
     * @throws NoSuchElementException when the document holds no shape at all.
     * @throws IllegalArgumentException when the document holds more than one shape, or its
     *   leaf is not an [N].
     * @throws SvgFormatException when a document violates the `/v1` format — every failure is a
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
     * Loads every shape of the SVG document at [path] as typed leaves, in document order —
     * the any-count [load]: `loadAll<PolygonNode>("files/tiles.svg")` yields a
     * `List<PolygonNode>`.
     *
     * The same found-vs-expected assertion applies to every leaf in the document; any shape
     * count is accepted — a whole layout can be one document, and a shapeless one yields an
     * empty list. The document loads exactly like [loadGroup], `<use>` templates included.
     *
     * @param N The expected concrete [ShapeNode] type of every leaf.
     * @param path The document's resource path.
     * @return The document's leaves as typed [N]s, in document order.
     * @throws IllegalArgumentException when any leaf in the document is not an [N].
     * @throws SvgFormatException when a document violates the `/v1` format — every failure is a
     *   subtype naming its condition.
     */
    public suspend inline fun <reified N : ShapeNode> loadAll(path: String): List<N> =
        allShapes(path).map { node ->
            require(node is N) {
                "asset $path contains ${node::class.simpleName}, expected ${N::class.simpleName}"
            }
            node
        }

    @PublishedApi
    internal suspend fun singleShape(path: String): ShapeNode =
        allShapes(path).single()

    @PublishedApi
    internal suspend fun allShapes(path: String): List<ShapeNode> =
        loadGroup(path).flattenShapes()
}

/**
 * Resolves a `<use>` href against the referencing document's directory — hrefs are plain
 * sibling file names; deliberately no `..`/absolute handling.
 */
internal fun siblingPath(documentPath: String, href: String): String {
    val dir = documentPath.substringBeforeLast('/', "")
    return if (dir.isEmpty()) href else "$dir/$href"
}
