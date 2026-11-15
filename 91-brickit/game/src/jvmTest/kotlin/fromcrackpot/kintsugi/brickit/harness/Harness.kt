package fromcrackpot.kintsugi.brickit.harness

import fromcrackpot.kintsugi.brickit.BrickIt
import fromcrackpot.kintsugi.brickit.BrickItAssets
import fromcrackpot.kintsugi.brickit.BrickItSounds
import fromcrackpot.kintsugi.gameengine.AssetScope
import fromcrackpot.kintsugi.gameengine.Sound
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlinx.coroutines.runBlocking
import java.io.File

// Test working dir varies by launcher (module dir under Gradle, repo root elsewhere), so probe both.
internal val moduleDir: File = File(System.getProperty("user.dir")).let { cwd ->
    if (cwd.resolve("src/commonMain/composeResources").isDirectory) cwd else cwd.resolve("91-brickit/game")
}

private fun scope() = AssetScope { path -> moduleDir.resolve("src/commonMain/composeResources/$path").readBytes() }

// Mirrors App()'s boot construction, duplicated by design so the game file carries no harness seam.
internal fun loadBrickIt(): BrickIt = runBlocking {
    scope().run {
        BrickIt(
            BrickItAssets(
                wall = loadAll<PolygonNode>("files/level-01.svg"),
                paddle = load<PolygonNode>("files/paddle.svg"),
                ball = load<PolygonNode>("files/ball.svg"),
                ballCharged = load<PolygonNode>("files/ball-charged.svg").style,
                sounds = loadSounds(),
            )
        )
    }
}

private suspend fun AssetScope.loadSounds() = BrickItSounds(
    paddle = loadSound("files/sounds/paddle.bfxr"),
    ballCharge = loadSound("files/sounds/ball-charge.bfxr"),
    wall = loadSound("files/sounds/wall.bfxr"),
    ballDischarge = loadSound("files/sounds/ball-discharge.bfxr"),
    brickSteel = loadSound("files/sounds/brick-steel.bfxr"),
    brickNormalShatter = loadSound("files/sounds/brick-normal-shatter.bfxr"),
    brickSteelShatter = loadSound("files/sounds/brick-steel-shatter.bfxr"),
    levelCompleted = loadSound("files/sounds/level-completed.bfxr"),
)

// Sounds compare by content, so one load names those of every world the harness builds.
internal val soundNames: Map<Sound, String> by lazy {
    runBlocking { scope().loadSounds() }.run {
        mapOf(
            paddle to "paddle", ballCharge to "ball-charge", wall to "wall", ballDischarge to "ball-discharge",
            brickSteel to "brick-steel", brickNormalShatter to "brick-normal-shatter",
            brickSteelShatter to "brick-steel-shatter", levelCompleted to "level-completed",
        )
    }
}
