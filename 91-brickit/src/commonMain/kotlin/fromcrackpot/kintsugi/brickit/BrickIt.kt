package fromcrackpot.kintsugi.brickit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.brickit.entities.*
import fromcrackpot.kintsugi.brickit.generated.resources.Res
import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.constraints.coerceIn
import fromcrackpot.kintsugi.cga.d2.constraints.coerceOn
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.d2.queries.collide
import fromcrackpot.kintsugi.cga.d2.queries.escape
import fromcrackpot.kintsugi.cga.d2.queries.invoke
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.TOP
import fromcrackpot.kintsugi.cga.d2.shapes.composites.get
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.*
import fromcrackpot.kintsugi.gameengine.dynamics.bounce
import fromcrackpot.kintsugi.gameengine.dynamics.times
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.math.sign

/* BOOT */

// The arena is game geometry like any other node; its bbox doubles as the scene bounds.
internal val ARENA = Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))

@Composable
fun App() = Game(modifier = Modifier.fillMaxSize().background(Color(0xFF2E3440))) {
    AssetScope(Res::readBytes).run {
        BrickIt(
            arena = PolygonNode(ARENA, ShapeStyle.POLYGON.copy(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE)),
            bricks = BrickWall(loadAll<PolygonNode>("files/wall.svg").map(::Brick)),
            paddle = Paddle(load<PolygonNode>("files/paddle.svg")),
            ball = Ball(load<PolygonNode>("files/ball.svg"), load<PolygonNode>("files/ball-charged.svg").style),
        )
    }
}

/* WORLD */

class BrickIt(
    internal val arena: PolygonNode,
    internal var bricks: BrickWall,
    internal var paddle: Paddle,
    internal var ball: Ball,
) : World(SceneBounds(arena)) {

    init { // Scene placement
        paddle *= T(0.0, PADDLE_Y)
        ball = ball coerceOn paddle
        bricks *= T(0.0, BRICKS_Y)
    }

    // Params order is draw order
    override val scene get() = GroupNode(arena, bricks, paddle, ball)

    private val paddleControl = FollowInputX(PADDLE_SPEED, left = Key.DirectionLeft, right = Key.DirectionRight)
    private val ballPaddleExtent = ball.edges[TOP].radius + paddle.edges[TOP].radius

    context(step: Step)
    override fun update() {
        // Move the paddle relatively to inputX
        paddle *= T(paddleControl.dx(paddle.x), 0.0)

        run { // Tilt servo: step the tilt toward its target at a bounded rate, then level off.
            // The bottom edge travels bl → br (factory order), so its attitude.angle is the tilt.
            val target: Angle = PADDLE_MAX_TILT * paddle.tiltTarget
            val current: Angle = paddle.edges[BOTTOM].attitude.angle
            val delta: Angle = target - current

            if (round(delta) != 0.rad) {
                val maxStep = PADDLE_TILT_RATE * step.dt
                paddle *= R(paddle.centroid, delta.coerceIn(-maxStep, maxStep))
            } else paddle.tiltTarget = 0.0 // reached the tilt — return to level
        }

        // Ensure paddle stay inside the arena
        paddle = paddle coerceIn arena

        // Move the ball
        ball *= T(ball.velocity * step.dt)

        // Paddle contact: a long face returns the ball by where it was hit, the hit end dips through the servo
        (ball collide paddle) { p ->
            val hit = ball.velocity dot p.normal < 0.0
            // Normalized impact offset from the paddle centre [-1, 1]
            val hitOffset = ((ball.x - paddle.x) / ballPaddleExtent).coerceIn(-1.0, 1.0)
            ball *= bounce(p)
            if (!hit) return@invoke // pushed by the swinging or sliding paddle

            paddle.tiltTarget = -sign(round(hitOffset))
            // Top discharges, the underside counts, a side charges at once; a long face returns by the hit point
            // at constant vertical speed — outward on top, inward underneath
            when (p.edge) {
                paddle.edges[TOP] -> { ball.charge = 0; ball.velocity = Direction(PADDLE_TIP_VX * hitOffset, BALL_SPEED) }
                paddle.edges[BOTTOM] -> { ball.charge++; ball.velocity = Direction(-PADDLE_TIP_VX * hitOffset, -BALL_SPEED) }
                else -> ball.charge = FULL_CHARGE
            }
        }

        // Check ball/brick collisions.
        for (brick in bricks) {
            (ball collide brick) { p ->
                if (brick.gold) restart()
                // charged: normal → flown through; steel → broken, bounced off, discharged
                if (!ball.charged || brick.steel) ball *= bounce(p)
                if (!brick.steel || ball.charged) bricks -= brick
                if (brick.steel && ball.charged) ball.charge = 0 // last: the removal reads charged
            }
        }

        // Arena boundary last: it always wins the step
        (ball escape arena) { p -> ball *= bounce(p) }
    }
}
