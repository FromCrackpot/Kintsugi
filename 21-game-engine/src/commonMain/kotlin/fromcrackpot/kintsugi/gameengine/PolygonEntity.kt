package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode

/**
 * The base of a game entity whose body is a polygon: a [PolygonNode] subclass, so the entity
 * *is* its node — geometry, style, and tags — plus whatever facts the game class adds.
 *
 * An entity is a value like any node: it moves by rebinding (`ball *= motion` replaces the
 * variable with the moved entity), and every shape-preserving operation returns through
 * [copy], which this class re-abstracts — the compiler forces each entity class to say how it
 * is rebuilt, carrying its facts across (`Ball(PolygonNode(polygon, style, tags)).also {
 * it.velocity = velocity }`). Facts a subclass adds (a velocity, a charge) are plain mutable
 * properties; the inherited identity stays structural and fact-exclusive — an entity that
 * needs fact-sensitive equality folds its facts in locally over `super.equals`.
 *
 * @constructor Roots the entity at [node]'s geometry, style, and tags.
 * @param node The node value the entity starts as.
 */
public abstract class PolygonEntity(node: PolygonNode) : PolygonNode(node), Entity {

    /** The entity's position on x: the x coordinate of its polygon's [centroid]. */
    public final override val x: Double get() = centroid.px

    /** The entity's position on y: the y coordinate of its polygon's [centroid]. */
    public final override val y: Double get() = centroid.py

    /**
     * Returns this entity rebuilt around [polygon] — the hook every shape-preserving
     * operation returns through, re-abstracted so each entity class must implement it.
     *
     * The implementation rebuilds an instance of the entity's own class around [polygon] and
     * carries its facts across; [style] and [tags] ride along through the node it wraps. The
     * result's runtime class must equal the receiver's — the operations check it and fail
     * with a named error otherwise.
     *
     * @param polygon The replacement geometry.
     * @return The rebuilt entity — the receiver's own class, facts carried.
     */
    public abstract override fun copy(polygon: Polygon): PolygonEntity
}
