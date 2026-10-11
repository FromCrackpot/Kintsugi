package fromcrackpot.kintsugi.brickit.harness

import fromcrackpot.kintsugi.brickit.ARENA
import fromcrackpot.kintsugi.brickit.BrickIt
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.Brick
import fromcrackpot.kintsugi.brickit.entities.BrickWall
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.gameengine.AssetScope
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlinx.coroutines.runBlocking
import java.io.File

// Test working dir varies by launcher (module dir under Gradle, repo root elsewhere) — probe both.
internal val moduleDir: File = File(System.getProperty("user.dir")).let { cwd ->
    if (cwd.resolve("src/commonMain/composeResources").isDirectory) cwd else cwd.resolve("91-brickit")
}

// Mirrors App()'s boot construction — duplicated by design so the game file carries no harness seam.
internal fun loadBrickIt(): BrickIt = runBlocking {
    AssetScope { path -> moduleDir.resolve("src/commonMain/composeResources/$path").readBytes() }.run {
        BrickIt(
            arena = PolygonNode(ARENA, ShapeStyle.POLYGON.copy(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE)),
            bricks = BrickWall(loadAll<PolygonNode>("files/wall.svg").map(::Brick)),
            paddle = Paddle(load<PolygonNode>("files/paddle.svg")),
            ball = Ball(load<PolygonNode>("files/ball.svg"), load<PolygonNode>("files/ball-charged.svg").style),
        )
    }
}
