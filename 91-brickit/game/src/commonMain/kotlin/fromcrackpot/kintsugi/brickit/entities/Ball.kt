package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.gameengine.entity.PolygonEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Inertial
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.restyled
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/* TUNING */

internal const val BALL_SPEED = 400.0 // launch, units/sec
internal const val FULL_CHARGE = 2 // paddle rebounds off the underside; a side charges at once

class Ball(
    node: PolygonNode,
    private val chargedStyle: ShapeStyle,
    private val plainStyle: ShapeStyle = node.style,
) : PolygonEntity(node), Inertial {

    override var velocity: Direction = Direction(BALL_SPEED, BALL_SPEED)

    var charge: Int = 0
        private set
    val charged: Boolean get() = charge >= FULL_CHARGE

    var loose: Boolean = false

    fun withCharge(charge: Int): Ball =
        restyled(if (charge >= FULL_CHARGE) chargedStyle else plainStyle).also { it.charge = charge }

    override fun rebuild(node: PolygonNode): Ball =
        Ball(node, chargedStyle, plainStyle).also { it.velocity = velocity; it.charge = charge; it.loose = loose }
}
