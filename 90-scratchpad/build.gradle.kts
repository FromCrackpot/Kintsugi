@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)

    jvm("desktop")
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":cga"))
            implementation(project(":scene-graph"))
            implementation(project(":compose"))
            implementation(project(":svg"))
            implementation(project(":game-engine"))
            implementation(project(":audio"))
            implementation(project(":bfxr"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    // Pin the generated Res class package (the default derives from project.group, which is unset).
    packageOfResClass = "fromcrackpot.kintsugi.scratchpad.generated.resources"
}

compose.desktop {
    application {
        mainClass = "fromcrackpot.kintsugi.scratchpad.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        // Sketch selection: `./gradlew :scratchpad:run -Psketch=logo`. Absent → default sketch.
        (project.findProperty("sketch") as? String)?.let { args += listOf(it) }
    }
}

// `compose.desktop.application.args` only configures the `run` task. The Compose Hot Reload
// plugin's hot-run tasks (hotRunDesktop / hotDevDesktop) are separate JavaExec tasks that take
// no args from that DSL, so mirror the sketch arg onto them here. Absent `-Psketch=` → default.
(project.findProperty("sketch") as? String)?.let { sketch ->
    tasks.withType<JavaExec>()
        .matching { it.name == "hotRunDesktop" || it.name == "hotDevDesktop" }
        .configureEach { args(sketch) }
}
