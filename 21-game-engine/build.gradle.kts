@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    `maven-publish`
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}

// -PgameDebug bakes the engine's debug facility (loop pause/step) into the build; absent, the
// generated constant is false and the debug code paths are compiled out (see Game.kt).
val gameDebug = providers.gradleProperty("gameDebug").isPresent

val generateDebugConfig by tasks.registering {
    val enabled = gameDebug
    val outDir = layout.buildDirectory.dir("generated/gameDebug/kotlin")
    inputs.property("enabled", enabled)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("fromcrackpot/kintsugi/gameengine/DebugConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package fromcrackpot.kintsugi.gameengine\n\ninternal const val GAME_DEBUG: Boolean = $enabled\n"
        )
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val debug = gameDebug
    doFirst { check(!debug) { "refusing to publish with -PgameDebug: GAME_DEBUG would ship baked in" } }
}

kotlin {
    jvmToolchain(25)
    explicitApi()

    jvm()
    android {
        namespace = "fromcrackpot.kintsugi.gameengine"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    wasmJs {
        browser()
        // Library exception to "binaries.executable() is for apps": CMP 1.12's
        // checkComposeUiTestConfigurationForWasmJs requires it so wasm tests bundle Skiko (CMP-4906).
        binaries.executable()
    }
    // -Pios: a compile-only check until the tests run on a simulator. Publishing refuses it (root build).
    if (providers.gradleProperty("ios").isPresent) {
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateDebugConfig)
            dependencies {
                // api: :game-engine is the SDK umbrella. Games depend on :game-engine alone and reach
                // every library type (Shape, ShapeGroup, Versor, …) through it.
                api(project(":cga"))
                api(project(":scene-graph"))
                api(project(":compose"))
                api(project(":svg"))
                api(project(":audio"))
                api(project(":bfxr"))
                api(compose.runtime)
                api(compose.foundation)
                implementation(libs.androidx.lifecycle.runtime.compose)
                api(compose.ui)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
