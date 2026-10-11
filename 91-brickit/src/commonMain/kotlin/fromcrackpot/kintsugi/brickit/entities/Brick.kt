package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.gameengine.PolygonEntity
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode

/* TUNING */

internal const val BRICKS_Y = 120.0

class Brick(node: PolygonNode) : PolygonEntity(node) {
    // steel deflects and survives an uncharged ball.
    val steel: Boolean = "steel" in tags
    val gold: Boolean = "gold" in tags

    override fun copy(polygon: Polygon): Brick = Brick(PolygonNode(polygon, style, tags))
}
