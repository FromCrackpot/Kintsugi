@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)

    jvm("desktop")
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(
                    open = !project.hasProperty("noOpen"),
                )
            }
        }
        binaries.executable()
    }
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()

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

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    // Pin the generated Res class package (the default derives from project.group, which is unset).
    packageOfResClass = "fromcrackpot.kintsugi.brickit.generated.resources"
}

compose.desktop {
    application {
        mainClass = "fromcrackpot.kintsugi.brickit.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
    }
}
