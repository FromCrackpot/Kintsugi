@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)

    jvm()
    android {
        namespace = "fromcrackpot.kintsugi.brickit.game"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true } // compose resources ride Android assets
    }
    wasmJs {
        browser()
        // Library exception: CMP 1.12's checkComposeUiTestConfigurationForWasmJs needs it so wasm tests bundle Skiko (CMP-4906).
        binaries.executable()
    }
    // -Pios: a compile-only check until the tests run on a simulator; the Xcode host (ios/) needs a Mac.
    if (providers.gradleProperty("ios").isPresent) {
        listOf(iosArm64(), iosSimulatorArm64()).forEach {
            it.binaries.framework {
                baseName = "BrickIt"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":game-engine"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    packageOfResClass = "fromcrackpot.kintsugi.brickit.generated.resources"
}
