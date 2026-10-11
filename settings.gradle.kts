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

include(":scratchpad")
project(":scratchpad").projectDir = file("90-scratchpad")

include(":brickit")
project(":brickit").projectDir = file("91-brickit")
