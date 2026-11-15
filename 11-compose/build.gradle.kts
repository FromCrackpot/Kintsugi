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

kotlin {
    jvmToolchain(25)
    explicitApi()

    jvm()
    android {
        namespace = "fromcrackpot.kintsugi.compose"
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
        commonMain.dependencies {
            api(project(":cga"))
            api(project(":scene-graph"))
            api(compose.runtime)
            implementation(compose.foundation)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            // Skia native runtime: CanvasDrawScope's draw routing creates a SkiaBackedPaint
            // on first draw, which fails without the host OS's Skiko natives on the classpath.
            // runtimeOnly: test sources reference no compose.desktop symbol at compile time;
            // this dep purely delivers the host-OS skiko-awt JAR for System.load at test exec.
            runtimeOnly(compose.desktop.currentOs)
        }
    }
}
