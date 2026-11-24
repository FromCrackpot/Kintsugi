rootProject.name = "kintsugi"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":cga")
project(":cga").projectDir = file("01-cga")

include(":scene-graph")
project(":scene-graph").projectDir = file("02-scene-graph")

include(":compose")
project(":compose").projectDir = file("11-compose")

include(":svg")
project(":svg").projectDir = file("12-svg")

include(":game-engine")
project(":game-engine").projectDir = file("21-game-engine")

include(":audio")
project(":audio").projectDir = file("31-audio")

include(":bfxr")
project(":bfxr").projectDir = file("32-bfxr")

include(":scratchpad")
project(":scratchpad").projectDir = file("90-scratchpad")

include(":brickit:game", ":brickit:desktop", ":brickit:web", ":brickit:android")
project(":brickit").projectDir = file("91-brickit")
project(":brickit:game").projectDir = file("91-brickit/game")
project(":brickit:desktop").projectDir = file("91-brickit/desktop")
project(":brickit:web").projectDir = file("91-brickit/web")
project(":brickit:android").projectDir = file("91-brickit/android")

if (file("92-docs-gallery").isDirectory) {
    include(":docs-gallery:cards", ":docs-gallery:web")
    project(":docs-gallery").projectDir = file("92-docs-gallery")
    project(":docs-gallery:cards").projectDir = file("92-docs-gallery/cards")
    project(":docs-gallery:web").projectDir = file("92-docs-gallery/web")
}
