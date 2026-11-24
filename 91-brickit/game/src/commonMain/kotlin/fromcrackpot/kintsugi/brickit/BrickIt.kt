package fromcrackpot.kintsugi.brickit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import fromcrackpot.kintsugi.brickit.entities.*
import fromcrackpot.kintsugi.brickit.generated.resources.Res
import fromcrackpot.kintsugi.cga.d2.operations.constraints.coerceIn
import fromcrackpot.kintsugi.cga.d2.operations.constraints.coerceOn
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.operations.products.times
import fromcrackpot.kintsugi.cga.d2.operations.queries.invoke
import fromcrackpot.kintsugi.cga.d2.operations.queries.onCollide
import fromcrackpot.kintsugi.cga.d2.operations.queries.onEscape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.TOP
import fromcrackpot.kintsugi.cga.d2.shapes.composites.get
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.*
import fromcrackpot.kintsugi.gameengine.entity.GroupEntity
import fromcrackpot.kintsugi.gameengine.entity.kinetics.bounce
import fromcrackpot.kintsugi.gameengine.entity.kinetics.times
import fromcrackpot.kintsugi.gameengine.entity.minus
import fromcrackpot.kintsugi.gameengine.entity.traits.explode
import fromcrackpot.kintsugi.gameengine.input.FollowInputX
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.math.sign

/* BOOT */

// The arena is game geometry like any other node; its bbox doubles as the scene bounds.
internal val ARENA = Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))
private val ARENA_STYLE = ShapeStyle.POLYGON.copy(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE)

// The ball-charged asset is loaded for its style only (the charged look).
class BrickItAssets(
    val wall: List<PolygonNode>,
    val paddle: PolygonNode,
    val ball: PolygonNode,
    val ballCharged: ShapeStyle,
    val sounds: BrickItSounds,
)

class BrickItSounds(
    val paddle: Sound,
    val ballCharge: Sound,
    val wall: Sound,
    val ballDischarge: Sound,
    val brickSteel: Sound,
    val brickNormalShatter: Sound,
    val brickSteelShatter: Sound,
    val levelCompleted: Sound,
)

@Composable
fun App(
    alignment: Alignment = Alignment.Center,
    onFirstFrame: () -> Unit = {},
    onFailure: (Throwable) -> Unit = {},
) = Game(
    modifier = Modifier.fillMaxSize().background(Color(0xFF2E3440)),
    alignment = alignment,
    preferredFrameRate = 90.0,
    onFirstFrame = onFirstFrame,
    onFailure = onFailure,
    pauseOverlay = {
        Spacer(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f)))
        BasicText("Touch to resume", Modifier.align(Alignment.Center), TextStyle(color = Color.White, fontSize = 32.sp))
    },
) {
    AssetScope(Res::readBytes).run {
        BrickIt(
            BrickItAssets(
                wall = loadAll<PolygonNode>("files/level-02.svg"),
                paddle = load<PolygonNode>("files/paddle.svg"),
                ball = load<PolygonNode>("files/ball.svg"),
                ballCharged = load<PolygonNode>("files/ball-charged.svg").style,
                sounds = BrickItSounds(
                    paddle = loadSound("files/sounds/paddle.bfxr"),
                    ballCharge = loadSound("files/sounds/ball-charge.bfxr"),
                    wall = loadSound("files/sounds/wall.bfxr"),
                    ballDischarge = loadSound("files/sounds/ball-discharge.bfxr"),
                    brickSteel = loadSound("files/sounds/brick-steel.bfxr"),
                    brickNormalShatter = loadSound("files/sounds/brick-normal-shatter.bfxr"),
                    brickSteelShatter = loadSound("files/sounds/brick-steel-shatter.bfxr"),
                    levelCompleted = loadSound("files/sounds/level-completed.bfxr"),
                ),
            )
        )
    }
}

/* WORLD */

class BrickIt(assets: BrickItAssets) : World(SceneBounds(ARENA)) {

    // Staging order is draw order
    internal val arena by staged(PolygonNode(ARENA, ARENA_STYLE))
    internal var bricks by staged(GroupEntity(assets.wall.map(::Brick)))
    internal var paddle by staged(Paddle(assets.paddle) * T(0.0, PADDLE_Y))
    internal var ball by staged(Ball(assets.ball, assets.ballCharged) coerceOn paddle)

    private val paddleControl =
        FollowInputX(PADDLE_SPEED, left = Key.DirectionLeft, right = Key.DirectionRight, dragGain = PADDLE_DRAG_GAIN)
    private val ballPaddleExtent = ball.edges[TOP].radius + paddle.edges[TOP].radius
    private val sounds = assets.sounds

    context(step: Step)
    override fun update() {
        // Move the paddle relatively to inputX
        paddle *= T(paddleControl.dx(paddle.x), 0.0)

        // Ensure paddle stay inside the arena
        paddle = paddle coerceIn arena

        // Move the ball
        ball *= T(ball.velocity * step.dt)

        // Paddle line: a ball climbing past it is "loose" until the paddle plays it;
        // a partial charge count is lost above it.
        val above = ball.y > paddle.y
        if (above && ball.velocity.py > 0.0) ball.loose = true
        if (above && ball.charge in 1 until FULL_CHARGE) ball = ball.withCharge(0)

        // Paddle contact: a long face returns the ball by where it was hit, the hit end dips through the servo
        (ball onCollide paddle) { p ->
            // Decide whether the ball is really hitting the paddle,
            // or whether the paddle moved into a ball that was already leaving.
            val hit = ball.velocity dot p.normal < 0.0
            // Normalized impact offset from the paddle centre [-1, 1]
            val hitOffset = ((ball.x - paddle.x) / ballPaddleExtent).coerceIn(-1.0, 1.0)

            ball *= bounce(p)

            if (!hit) return@invoke // pushed by the swinging or sliding paddle

            sounds.paddle.play()
            val wasCharged = ball.charged
            paddle.tiltTarget = PADDLE_MAX_TILT * -sign(round(hitOffset))
            // The underside counts up to a full charge, a side charges at once, both play the ball;
            // a long face returns by the hit point at constant vertical speed: outward on top, inward underneath
            when (p.edge) {
                paddle.edges[TOP] -> ball.velocity = Direction(PADDLE_TIP_VX * hitOffset, BALL_SPEED)
                paddle.edges[BOTTOM] -> {
                    ball = ball.withCharge((ball.charge + 1).coerceAtMost(FULL_CHARGE))
                    ball.loose = false
                    ball.velocity = Direction(-PADDLE_TIP_VX * hitOffset, -BALL_SPEED)
                }
                else -> {
                    ball = ball.withCharge(FULL_CHARGE);
                    ball.loose = false
                }
            }
            if (ball.charged && !wasCharged) sounds.ballCharge.play()
        }

        // Check ball/brick collisions, deepest penetration first.
        val contacts = bricks.children.mapNotNull { brick -> (ball onCollide brick)?.let { brick to it.depth } }
        for ((brick, _) in contacts.sortedByDescending { it.second }) {
            (ball onCollide brick) { p ->
                if (brick.gold) {
                    sounds.levelCompleted.play()
                    restart()
                }
                val hit = ball.velocity dot p.normal < 0.0
                // charged ball:
                // normal brick → flown through;
                // steel brick → broken and bounced off, the charge kept
                if (!ball.charged || brick.steel) ball *= bounce(p)
                if (!brick.steel || ball.charged) {
                    bricks -= brick
                    brick.explode()
                    (if (brick.steel) sounds.brickSteelShatter else sounds.brickNormalShatter).play()
                } else if (hit) sounds.brickSteel.play()
            }
        }

        // Arena boundary last: it always wins the step; the floor discharge a loose ball
        (ball onEscape arena) { p ->
            val hit = ball.velocity dot p.normal < 0.0
            ball *= bounce(p)
            if (hit) sounds.wall.play()
            if (ball.loose && p.edge == arena.edges[BOTTOM]) {
                if (ball.charged) sounds.ballDischarge.play()
                ball = ball.withCharge(0)
            }
        }
    }
}
