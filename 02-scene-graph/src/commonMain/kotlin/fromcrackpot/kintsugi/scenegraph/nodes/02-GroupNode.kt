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
 * A sub-tree of the scene graph: an ordered list of child nodes with an optional [bounds]
 * rectangle. A whole scene is a `GroupNode`.
 *
 * - Purely structural, with no style of its own. For a scene background, put a [backdrop] first.
 * - Group-preserving operations return through [copy], so a subclass comes back as its own class.
 *
 * @constructor Builds the group over its children and optional bounds.
 */
public open class GroupNode(
    /** The child nodes in paint order. Open, so a subclass may narrow the element type. */
    public open val children: List<Node> = emptyList(),
    /** The rectangle this sub-tree occupies, or `null` when unspecified. On a scene root, it is the viewport. */
    public val bounds: SceneBounds? = null,
) : Node {

    /**
     * Builds a group of `children` in the given order, without bounds.
     *
     * @param children The child nodes in paint order.
     */
    public constructor(vararg children: Node) : this(children.toList())

    /**
     * Returns a group with [children] and [bounds] in place of this group's own. Every
     * group-preserving operation returns through this rebuild hook.
     *
     * - Override it to rebuild your subclass. The result's runtime class must equal the receiver's.
     *
     * @param children The replacement children, this group's own when omitted.
     * @param bounds The replacement bounds, this group's own when omitted.
     * @return The rebuilt group.
     */
    public open fun copy(
        children: List<Node> = this.children,
        bounds: SceneBounds? = this.bounds,
    ): GroupNode = GroupNode(children, bounds)

    /**
     * Returns whether [other] is a group of the same exact class with equal [children] and [bounds].
     *
     * - Structural: children compare pairwise, leaves by their style-inclusive identity, groups recursively.
     *
     * @param other The object to compare against.
     * @return Whether the two groups are structurally equal.
     */
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is GroupNode && this::class == other::class && children == other.children && bounds == other.bounds)

    /** Returns the hash of [children] combined with [bounds]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * children.hashCode() + (bounds?.hashCode() ?: 0)

    /** Returns the children and bounds, the structural content [equals] compares. */
    override fun toString(): String = "GroupNode(children=$children, bounds=$bounds)"
}

/**
 * Returns every [ShapeNode] in the tree, depth-first in source order. The result is in paint
 * order.
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
 * - Structure, order, styles and tags are kept. [GroupNode.bounds] does not move.
 *
 * @receiver The group to move.
 * @param G The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The rebuilt group, every leaf moved.
 * @throws DegeneratePolylineException when a polyline leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
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
 * Builds a group holding one full-bleed [color]-filled rectangle over [bounds]. Use it as a
 * placeholder scene, or as a scene's first child for a backdrop.
 *
 * - The rectangle is origin-centered and ignores the bounds' offset. It covers exactly what renders.
 *
 * @param bounds The viewport to cover; also the group's own bounds.
 * @param color The fill, opaque black by default.
 * @return The one-child group.
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
 * Returns the tree's single leaf narrowed to its concrete node type [N]. It is the one runtime
 * assertion at an asset's load site.
 *
 * @param N The expected concrete leaf type.
 * @receiver The single-leaf tree to narrow.
 * @return The leaf as [N].
 * @throws NoSuchElementException when the tree holds no leaf at all.
 * @throws IllegalArgumentException when the tree holds more than one leaf.
 * @throws IllegalStateException when the single leaf is not an [N].
 */
public inline fun <reified N : ShapeNode> GroupNode.single(): N {
    val leaf = flattenShapes().single()
    return leaf as? N
        ?: error("asset contains ${leaf::class.simpleName}, expected ${N::class.simpleName}")
}

/** Converts a wrong-class `copy` result into an immediate named error. The group ops' only unchecked cast. */
internal fun <G : GroupNode> G.narrowed(copied: GroupNode): G {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName}, not its own class"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as G
}
