plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
    }
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
