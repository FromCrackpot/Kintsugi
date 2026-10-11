plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
    wasmJs {
        browser()
        // Library exception to "binaries.executable() is for apps": CMP 1.12's
        // checkComposeUiTestConfigurationForWasmJs requires it so wasm tests bundle Skiko (CMP-4906).
        binaries.executable()
    }
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

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
            // Skia native runtime — CanvasDrawScope's draw routing creates a SkiaBackedPaint
            // on first draw, which fails without the host OS's Skiko natives on the classpath.
            // runtimeOnly: test sources reference no compose.desktop symbol at compile time;
            // this dep purely delivers the host-OS skiko-awt JAR for System.load at test exec.
            runtimeOnly(compose.desktop.currentOs)
        }
    }
}
