package fromcrackpot.kintsugi.gameengine.entity

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.times

/**
 * The base of a game entity whose body is a group of nodes. It is a [GroupNode] subclass, so the
 * entity *is* its sub-tree (the typed [children] in draw order) plus the game class's facts.
 *
 * - A value that moves by rebinding (`wall *= motion`). Each class implements [rebuild].
 * - Identity stays structural and ignores the facts a subclass adds.
 *
 * @param E The children's type.
 * @constructor Roots the entity at its children, in draw order, with optional bounds.
 */
public abstract class GroupEntity<E : Node>(
    /** The group's entities in draw order, the typed narrowing of [GroupNode.children]. */
    // Shadows the base backing field by design, the price of covariant narrowing. Don't fold into a cast.
    final override val children: List<E>,
    bounds: SceneBounds? = null,
) : GroupNode(children, bounds) {

    /**
     * Returns this entity's own class around [children], its facts carried across. Every
     * group-preserving operation returns through this one rebuild.
     *
     * @param children The replacement children, in draw order.
     * @return The rebuilt entity: the receiver's own class, facts carried.
     */
    protected abstract fun rebuild(children: List<E>): GroupEntity<E>

    /**
     * Returns this entity around [children] through [rebuild]. [bounds] is ignored.
     *
     * @param children The replacement children.
     * @param bounds Ignored. An entity's bounds are its own class's concern.
     * @return The rebuilt entity: the receiver's own class, facts carried.
     */
    final override fun copy(children: List<Node>, bounds: SceneBounds?): GroupEntity<E> {
        // Safe by invariant: the group ops feed each child through its own copy hook, already
        // class-checked by its narrowed, so every element here is the E that went in.
        @Suppress("UNCHECKED_CAST")
        return rebuild(children as List<E>)
    }
}

/**
 * Builds a group entity over [children] with no facts of its own, for a group the game needn't
 * name a class for.
 *
 * - Every group operation returns this same kind of group. A group with facts subclasses [GroupEntity].
 *
 * @param E The children's type.
 * @param children The group's entities in draw order.
 * @return The group entity.
 */
public fun <E : Node> GroupEntity(children: List<E>): GroupEntity<E> =
    object : GroupEntity<E>(children) {
        override fun rebuild(children: List<E>): GroupEntity<E> = GroupEntity(children)
    }

/**
 * `GroupEntity * Versor => GroupEntity`<br>
 * Returns this entity with [versor]'s motion applied to every leaf in its sub-tree.
 *
 * @receiver The entity to move.
 * @param G The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved entity, facts carried.
 * @throws DegeneratePolylineException when a polyline leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move makes two consecutive
 *   vertices round equal at the `SCALE` quantum.
 */
// The body's `*` resolves to the explicitly imported scene-graph generic (imports outrank
// same-file declarations). That import is what keeps this from recursing into itself.
public operator fun <G : GroupEntity<*>> G.times(versor: Versor): G = this * versor

/**
 * `GroupEntity - Node => GroupEntity`<br>
 * Returns this entity without [child] among its children. `group -= child` removes and
 * rebinds.
 *
 * - Matched by identity (`===`): two geometrically identical children are distinct members.
 * - Removing an absent child returns this entity itself.
 *
 * @receiver The entity to remove [child] from.
 * @param G The receiver's type, preserved in the result.
 * @param E The children's type.
 * @param child The child to remove.
 * @return The rebuilt entity, or this entity itself when [child] is not a member.
 */
public operator fun <G : GroupEntity<E>, E : Node> G.minus(child: E): G {
    val survivors = children.filterNot { it === child }
    if (survivors.size == children.size) return this
    return narrowed(copy(children = survivors))
}

/**
 * Runs [action] on each child, in draw order.
 *
 * - Rebinding the group's `var` inside [action] never disturbs the iteration.
 *
 * @receiver The entity whose children to visit.
 * @param E The children's type.
 * @param action The function to run on each child. An exception it throws propagates.
 */
public inline fun <E : Node> GroupEntity<E>.forEach(action: (E) -> Unit): Unit =
    children.forEach(action)

/**
 * Returns an iterator over the children, in draw order. It makes `for (child in group)` work.
 *
 * - The group is not `Iterable`: for collection operations, use [GroupEntity.children].
 *
 * @receiver The entity whose children to iterate.
 * @param E The children's type.
 * @return An iterator over the children.
 */
public operator fun <E : Node> GroupEntity<E>.iterator(): Iterator<E> = children.iterator()

/** Converts a wrong-class `copy` result into an immediate named error. This is the removal verb's one cast site. */
internal fun <G : GroupEntity<*>> G.narrowed(copied: GroupEntity<*>): G {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName}, not its own class"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as G
}
