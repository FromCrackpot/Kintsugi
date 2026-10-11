import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    dokka(project(":cga"))
    dokka(project(":scene-graph"))
    dokka(project(":compose"))
    dokka(project(":svg"))
    dokka(project(":game-engine"))
}

dokka {
    moduleName.set("Kintsugi")
}

subprojects {
    val moduleName = name
    plugins.withId("maven-publish") {
        // published .module files name each file after its archive → archives carry the kintsugi- prefix too
        plugins.withType<BasePlugin> {
            extensions.configure<BasePluginExtension> { archivesName.set("kintsugi-$moduleName") }
        }
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "release"
                    url = uri(rootProject.layout.buildDirectory.dir("release-repo"))
                }
            }
            publications.withType<MavenPublication>().configureEach {
                if (name == "kotlinMultiplatform") artifactId = "kintsugi-$moduleName"
                pom {
                    name.set("kintsugi-$moduleName")
                    url.set("https://kintsugi-engine.com")
                    licenses {
                        license {
                            name.set("Kintsugi Engine Community License 1.0")
                            url.set("https://kintsugi-engine.com/licenses/community/1.0")
                        }
                    }
                    developers {
                        developer {
                            id.set("fromcrackpot")
                            name.set("fromcrackpot")
                            organization.set("fromcrackpot")
                        }
                    }
                }
            }
        }
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                // KGP assigns target artifactIds after publications.configureEach runs; mavenPublication {} runs after that
                targets.configureEach {
                    mavenPublication {
                        if (!artifactId.startsWith("kintsugi-")) artifactId = "kintsugi-$artifactId"
                    }
                }
            }
        }
    }
}
