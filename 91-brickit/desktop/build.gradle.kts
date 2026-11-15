plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":brickit:game"))
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "fromcrackpot.kintsugi.brickit.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
    }
}
