package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.gameengine.PolygonEntity
import fromcrackpot.kintsugi.gameengine.traits.Kinetic
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/* TUNING */

internal const val BALL_SPEED = 400.0 // launch, units/sec
internal const val FULL_CHARGE = 2 // paddle rebounds off the underside; a side charges at once

class Ball(node: PolygonNode, private val chargedStyle: ShapeStyle) : PolygonEntity(node), Kinetic {
    private val plainStyle = node.style

    override var velocity: Direction = Direction(BALL_SPEED, BALL_SPEED)

    var charge: Int = 0
        set(value) {
            field = value
            style = if (charged) chargedStyle else plainStyle
        }
    val charged: Boolean get() = charge >= FULL_CHARGE

    // plainStyle, not style: a charged ball wears chargedStyle — the carried charge re-applies it
    override fun copy(polygon: Polygon): Ball =
        Ball(PolygonNode(polygon, plainStyle, tags), chargedStyle)
            .also { it.velocity = velocity; it.charge = charge }
}
