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

val notices = tasks.register<Sync>("notices") {
    into(layout.buildDirectory.dir("notices"))
    into("META-INF/kintsugi-bfxr") {
        from(rootProject.file("NOTICE"))
        into("LICENSES") { from(rootProject.file("LICENSES/MIT.txt"), rootProject.file("LICENSES/Apache-2.0.txt")) }
    }
}

// the jvm.tasks base type: KGP's sources jars are not bundling.Jar
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach { from(notices) }

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/license/mit")
                    comments.set("bfxr2's synths, ported to Kotlin: Copyright (c) 2021 Stephen Lavelle")
                }
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    comments.set("The classic Bfxr synth, derived from SfxrSynth: Copyright 2010 Thomas Vian")
                }
            }
        }
    }
}

kotlin {
    jvmToolchain(25)
    explicitApi()

    jvm()
    android {
        namespace = "fromcrackpot.kintsugi.bfxr"
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
        androidMain { resources.srcDir(notices) }
        commonMain.dependencies {
            api(project(":audio"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
