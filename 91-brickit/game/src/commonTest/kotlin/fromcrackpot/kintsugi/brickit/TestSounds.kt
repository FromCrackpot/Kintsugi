package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.audio.Pcm
import fromcrackpot.kintsugi.gameengine.Sound

private fun tone(level: Float) = Sound(Pcm(floatArrayOf(level), 44_100))

// Eight distinct one-sample sounds: equality tells them apart.
internal val TEST_SOUNDS = BrickItSounds(
    paddle = tone(0.1f),
    ballCharge = tone(0.2f),
    wall = tone(0.3f),
    ballDischarge = tone(0.4f),
    brickSteel = tone(0.5f),
    brickNormalShatter = tone(0.6f),
    brickSteelShatter = tone(0.7f),
    levelCompleted = tone(0.8f),
)
