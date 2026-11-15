package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.gameengine.entity.PolygonEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Tilting
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.math.tan

/* TUNING */

internal const val PADDLE_Y = -230.0
internal const val PADDLE_SPEED = 800.0 // arrow-key slide, units/sec
internal const val PADDLE_DRAG_GAIN = 2.5 // touch: paddle units per dragged unit

internal val PADDLE_MAX_TILT = 30.deg
// Angular speed of the paddle tilt, per second.
val PADDLE_TILT_RATE = PADDLE_MAX_TILT * 12.0
// Tip return's horizontal speed: a vertical drop mirrored off the fully tilted paddle leaves 2·tilt off vertical
internal val PADDLE_TIP_VX = BALL_SPEED * tan((PADDLE_MAX_TILT * 2.0).radians)

class Paddle(node: PolygonNode) : PolygonEntity(node), Tilting {

    override var tiltTarget: Angle = 0.rad
    override val tiltRate: Angle get() = PADDLE_TILT_RATE

    override fun rebuild(node: PolygonNode): Paddle = Paddle(node).also { it.tiltTarget = tiltTarget }
}
