plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

base {
    archivesName = "brickit"
}

android {
    namespace = "fromcrackpot.kintsugi.brickit.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.fromcrackpot.brickit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        val (major, minor, patch) = version.toString().substringBefore('-').split('.').map(String::toInt)
        versionCode = major * 10_000 + minor * 100 + patch // 0.1.2 → 102
        versionName = version.toString()
    }
}

dependencies {
    implementation(project(":brickit:game"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
}
