package fromcrackpot.kintsugi.gameengine.entity

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * The base of a game entity whose body is a polygon. It is a [PolygonNode] subclass, so the entity
 * *is* its node (geometry, style and tags) plus the game class's facts.
 *
 * - A value that moves by rebinding (`ball *= motion`). Each class implements [rebuild].
 * - Identity stays structural and ignores added facts. Fold them in over `super.equals` if needed.
 *
 * @constructor Roots the entity at `node`'s geometry, style, and tags.
 * @param node The node value the entity starts as.
 */
public abstract class PolygonEntity(node: PolygonNode) : PolygonNode(node), Entity {

    /** The entity's position on x: the x coordinate of its polygon's [centroid]. */
    public final override val x: Double get() = centroid.px

    /** The entity's position on y: the y coordinate of its polygon's [centroid]. */
    public final override val y: Double get() = centroid.py

    /**
     * Returns this entity's own class around [node], its facts carried across. Every
     * shape-preserving operation, restyle and retag returns through this one rebuild.
     *
     * - Wrap [node] as given: its geometry, style, and tags are already the result's.
     *
     * @param node The rebuilt node.
     * @return The rebuilt entity: the receiver's own class, facts carried.
     */
    protected abstract fun rebuild(node: PolygonNode): PolygonEntity

    /**
     * Returns this entity around [polygon] through [rebuild], style and tags kept.
     *
     * @param polygon The replacement geometry.
     * @return The rebuilt entity.
     */
    final override fun copy(polygon: Polygon): PolygonEntity = rebuild(PolygonNode(polygon, style, tags))

    /**
     * Returns this entity with [style] and [tags] through [rebuild], geometry kept.
     *
     * @param style The concrete paint.
     * @param tags Free-form tokens riding the entity.
     * @return The rebuilt entity.
     */
    final override fun copy(style: ShapeStyle, tags: Set<String>): PolygonEntity =
        rebuild(PolygonNode(this, style, tags))
}
