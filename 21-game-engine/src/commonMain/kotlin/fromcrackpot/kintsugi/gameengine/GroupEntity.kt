package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.times

/**
 * The base of a game entity whose body is a group of nodes: a [GroupNode] subclass, so the
 * entity *is* its sub-tree — the typed [children] in draw order — plus whatever facts the
 * game class adds.
 *
 * The group analog of [PolygonEntity]: the entity is a value that moves by rebinding
 * (`wall *= motion` replaces the variable with the moved entity), and every group-preserving
 * operation returns through the typed [copy] hook, which the compiler forces each entity
 * class to implement — rebuilding its own class around the replacement children and carrying
 * its facts across. [children] covariantly narrows [GroupNode.children], so the group's
 * entities read back typed, cast-free. Facts a subclass adds are plain mutable properties;
 * the inherited identity stays structural and fact-exclusive.
 *
 * @param E The children's type.
 * @constructor Roots the entity at its children, in draw order, with optional bounds.
 */
public abstract class GroupEntity<E : Node>(
    /** The group's entities in draw order — the typed narrowing of [GroupNode.children]. */
    // Shadows the base backing field by design — the price of covariant narrowing; don't fold into a cast.
    final override val children: List<E>,
    bounds: SceneBounds? = null,
) : GroupNode(children, bounds) {

    /**
     * Returns this entity rebuilt around [children] — the hook every group-preserving
     * operation returns through, re-abstracted with typed children so each entity class must
     * implement it.
     *
     * The implementation rebuilds an instance of the entity's own class around [children] and
     * carries its facts across. The result's runtime class must equal the receiver's — the
     * operations check it and fail with a named error otherwise.
     *
     * @param children The replacement children.
     * @return The rebuilt entity — the receiver's own class, facts carried.
     */
    public abstract fun copy(children: List<E>): GroupEntity<E>

    /**
     * Returns this entity rebuilt around [children] through the typed hook; [bounds] is not
     * consulted — the rebuilt entity carries whatever bounds its own class sets.
     *
     * @param children The replacement children.
     * @param bounds Ignored; an entity's bounds are its own class's concern.
     * @return The rebuilt entity — the receiver's own class, facts carried.
     */
    final override fun copy(children: List<Node>, bounds: SceneBounds?): GroupEntity<E> {
        // Safe by invariant: the group ops feed each child through its own copy hook, already
        // class-checked by its narrowed — every element here is the E that went in.
        @Suppress("UNCHECKED_CAST")
        return copy(children as List<E>)
    }
}

/**
 * `GroupEntity * Versor => GroupEntity`<br>
 * Returns this entity with [versor]'s motion applied to every leaf in its sub-tree — the
 * entity-narrowed form of the scene graph's `GroupNode * Versor`.
 *
 * The result is the receiver's own class, its facts carried by the typed copy hook. The
 * narrowed receiver bound lets a game file import this form beside the leaf transform
 * operators without them contending for leaf-typed receivers.
 *
 * @receiver The entity to move.
 * @param G The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved entity, as the receiver's class.
 * @throws DegeneratePolylineException when a polyline leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 * @throws DegeneratePolygonException when a polygon leaf's move lands two consecutive
 *   vertices rounded-coincident at the `SCALE` quantum.
 */
// The body's `*` resolves to the explicitly imported scene-graph generic (imports outrank
// same-file declarations) — that import is what keeps this from recursing into itself.
public operator fun <G : GroupEntity<*>> G.times(versor: Versor): G = this * versor

/**
 * `GroupEntity - Node => GroupEntity`<br>
 * Returns this entity without [child] among its children — a child leaves the group by the
 * group being rebuilt without it.
 *
 * The removed child is found by identity (`===`), never by structural equality — two
 * geometrically identical children are distinct members. The result returns through the
 * typed copy hook, so it is the receiver's own class with its facts carried. When [child]
 * is not among the children the receiver itself is returned (`===`), unchanged — removing
 * an absent child is a no-op, so no call needs a membership pre-check. `group -= child`
 * spells remove-and-rebind at a `var` site.
 *
 * @receiver The entity to remove [child] from.
 * @param G The receiver's type, preserved in the result.
 * @param E The children's type.
 * @param child The child to remove, matched by identity.
 * @return The rebuilt entity without [child], as the receiver's class — or this entity
 *   itself when [child] is not a member.
 */
public operator fun <G : GroupEntity<E>, E : Node> G.minus(child: E): G {
    val survivors = children.filterNot { it === child }
    if (survivors.size == children.size) return this
    return narrowed(copy(survivors))
}

/**
 * Runs [action] on each child, in draw order.
 *
 * The group iterates directly — it is deliberately not `Iterable`, so the stdlib collection
 * extensions do not attach; [GroupEntity.children] is the surface for everything beyond
 * iteration. The children visited are this value's own immutable list, so rebinding a `var`
 * holding this group from inside [action] never disturbs the iteration. An exception thrown
 * by [action] propagates unchanged.
 *
 * @receiver The entity whose children to visit.
 * @param E The children's type.
 * @param action The function to run on each child.
 */
public inline fun <E : Node> GroupEntity<E>.forEach(action: (E) -> Unit): Unit =
    children.forEach(action)

/**
 * Returns an iterator over the children, in draw order.
 *
 * `for (child in group)` spells the iteration — the operator alone enables the `for`
 * statement, without the group being `Iterable`; the stdlib collection extensions do not
 * attach, and [GroupEntity.children] is the surface for everything beyond iteration.
 *
 * @receiver The entity whose children to iterate.
 * @param E The children's type.
 * @return An iterator over the children.
 */
public operator fun <E : Node> GroupEntity<E>.iterator(): Iterator<E> = children.iterator()

/** Converts a wrong-class typed-`copy` result into an immediate named error. */
internal fun <G : GroupEntity<*>> G.narrowed(copied: GroupEntity<*>): G {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName} — override copy in ${this::class.simpleName}"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as G
}
