@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
        namespace = "fromcrackpot.kintsugi.svg"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    wasmJs {
        browser()
    }
    // -Pios: a compile-only check until the tests run on a simulator. Publishing refuses it (root build).
    if (providers.gradleProperty("ios").isPresent) {
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":cga"))
            api(project(":scene-graph"))
            implementation(libs.xmlutil.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
