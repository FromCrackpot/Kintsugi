@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    wasmJs {
        browser {
            val open = !providers.gradleProperty("noOpen").isPresent
            commonWebpackConfig {
                outputFileName = "brickit.js" // index.html loads it
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(open = open)
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":brickit:game"))
            implementation(compose.ui)
        }
    }
}
