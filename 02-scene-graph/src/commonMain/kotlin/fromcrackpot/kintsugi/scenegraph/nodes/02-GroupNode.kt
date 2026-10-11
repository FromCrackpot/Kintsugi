package fromcrackpot.kintsugi.scenegraph.nodes

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A sub-tree of the scene graph: an ordered list of child nodes, with an optional [bounds]
 * rectangle. A whole scene is just a `GroupNode` — the root's [bounds] is the viewport
 * renderers paint into.
 *
 * Groups are purely structural, with no style of their own: every [ShapeNode] leaf carries
 * its own concrete style. There is no background field either — a scene backdrop is an
 * ordinary full-bleed rectangle as the first child, prebuilt by [backdrop].
 *
 * The class is open, and every group-preserving operation returns through the [copy] hook —
 * a subclass that overrides it comes back as its own class from each of them. Identity is
 * exact-class and structural across [children] and [bounds]; see [equals].
 *
 * @constructor Builds the group over its children and optional bounds.
 */
public open class GroupNode(
    /** The child nodes in source order — the order renderers paint them; open so a subclass may narrow the element type. */
    public open val children: List<Node> = emptyList(),
    /** The rectangle this sub-tree occupies — on a scene root, the viewport; `null` when unspecified. */
    public val bounds: SceneBounds? = null,
) : Node {

    /**
     * Builds a group of [children] in the given order, without bounds.
     *
     * @param children The child nodes in paint order.
     */
    public constructor(vararg children: Node) : this(children.toList())

    /**
     * Returns a group with [children] and [bounds] in place of this group's own — the rebuild
     * hook every group-preserving operation returns through.
     *
     * The base implementation builds a plain [GroupNode]. A subclass overrides the hook to
     * rebuild its own class around the replacement children, and then comes back as itself
     * from every group-preserving operation. The result's runtime class must equal the
     * receiver's — the operations check it and fail with a named error otherwise.
     *
     * @param children The replacement children; this group's own when omitted.
     * @param bounds The replacement bounds; this group's own when omitted.
     * @return The rebuilt group.
     */
    public open fun copy(
        children: List<Node> = this.children,
        bounds: SceneBounds? = this.bounds,
    ): GroupNode = GroupNode(children, bounds)

    /**
     * Returns whether [other] is a group of the same class with equal [children] and [bounds].
     *
     * Structural: the children lists compare pairwise, each leaf by its own style-inclusive
     * identity, nested groups recursively. Identity is exact-class: a subclass instance and a
     * plain [GroupNode] never compare equal, in either direction.
     *
     * @param other The object to compare against.
     * @return Whether the two groups are structurally equal.
     */
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is GroupNode && this::class == other::class && children == other.children && bounds == other.bounds)

    /** Returns the hash of [children] combined with [bounds]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * children.hashCode() + (bounds?.hashCode() ?: 0)

    /** Returns the children and bounds — the structural content [equals] compares. */
    override fun toString(): String = "GroupNode(children=$children, bounds=$bounds)"
}

/**
 * Returns every [ShapeNode] in the tree — a depth-first, source-order traversal, so the
 * resulting order is the paint order.
 *
 * @receiver The tree to traverse.
 * @return The tree's leaves, each carrying its own style and tags.
 */
public fun GroupNode.flattenShapes(): List<ShapeNode> {
    val out = mutableListOf<ShapeNode>()

    fun walk(group: GroupNode) {
        for (node in group.children) when (node) {
            is ShapeNode -> out += node
            is GroupNode -> walk(node)
        }
    }

    walk(this)

    return out
}

/**
 * `GroupNode * Versor => GroupNode`<br>
 * Returns this group with [versor]'s motion applied to every leaf in the sub-tree.
 *
 * The rebuild recurses into nested groups, preserving structure, source order, and each
 * leaf's style and tags; [GroupNode.bounds] passes through unchanged, so the content moves
 * within the same viewport. The result is the receiver's own class: the moved children
 * return through [GroupNode.copy], preserving a subclass that overrides the hook — and a
 * nested subclassed group is preserved the same way, through its own hook.
 *
 * @receiver The group to move.
 * @param G The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The rebuilt group, as the receiver's class — every leaf moved.
 * @throws DegeneratePolylineException when a polyline leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 */
public operator fun <G : GroupNode> G.times(versor: Versor): G =
    narrowed(
        copy(
            children = children.map { node ->
                when (node) {
                    is ShapeNode -> node * versor
                    is GroupNode -> node * versor
                }
            },
        ),
    )

/**
 * Builds a group holding one full-bleed [color]-filled rectangle over [bounds] — a
 * prebuilt scene backdrop, usable directly as a placeholder scene or nested as a scene's
 * first child.
 *
 * The rectangle is an origin-centered `width × height` quad, ignoring [SceneBounds.minX]
 * and [SceneBounds.minY] — the renderers' offset-ignoring clip convention — so it exactly
 * covers what renders. It is fill-only: [color] fills, no stroke.
 *
 * @param bounds The viewport to cover; also set as the group's own [GroupNode.bounds].
 * @param color The fill, opaque black by default.
 * @return The one-child [GroupNode].
 */
public fun backdrop(bounds: SceneBounds, color: ShapeColor = ShapeColor.BLACK): GroupNode =
    GroupNode(
        children = listOf(
            PolygonNode(
                Polygon.Rectangle(
                    FlatPoint(-bounds.width / 2, -bounds.height / 2),
                    FlatPoint(bounds.width / 2, bounds.height / 2),
                ),
                ShapeStyle.POLYGON.copy(fill = color, stroke = ShapeColor.NONE),
            ),
        ),
        bounds = bounds,
    )

/**
 * Returns the tree's single leaf narrowed to its concrete node type [N].
 *
 * The load-boundary idiom: what an externally loaded asset tree contains is unknowable at
 * compile time, so this places the one unavoidable runtime assertion at the load site —
 * everything downstream works on the typed leaf cast-free, style and tags riding along.
 *
 * @param N The expected concrete [ShapeNode] type.
 * @receiver The single-leaf tree to narrow.
 * @return The leaf as [N].
 * @throws NoSuchElementException when the tree holds no leaf at all.
 * @throws IllegalArgumentException when the tree holds more than one leaf.
 * @throws IllegalStateException when the single leaf is not an [N] — the message names the
 *   found and expected types.
 */
public inline fun <reified N : ShapeNode> GroupNode.single(): N {
    val leaf = flattenShapes().single()
    return leaf as? N
        ?: error("asset contains ${leaf::class.simpleName}, expected ${N::class.simpleName}")
}

/** Converts a wrong-class `copy` result into an immediate named error — the single unchecked-cast site of the group ops. */
internal fun <G : GroupNode> G.narrowed(copied: GroupNode): G {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName} — override copy in ${this::class.simpleName}"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as G
}
