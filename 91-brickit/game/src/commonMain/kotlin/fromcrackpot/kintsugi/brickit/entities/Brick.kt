package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.gameengine.entity.PolygonEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Explosive
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode

/* TUNING */

internal const val BRICK_FRAGMENTS = 8

class Brick(node: PolygonNode) : PolygonEntity(node), Explosive {
    // steel deflects and survives an uncharged ball.
    val steel: Boolean = "steel" in tags
    val gold: Boolean = "gold" in tags

    override val fragmentCount: Int get() = BRICK_FRAGMENTS

    override fun rebuild(node: PolygonNode): Brick = Brick(node)
}
