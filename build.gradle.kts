plugins {
    // Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Kotlin
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false

    // Build Tools
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.androidx.room) apply false

    // Code Quality
    alias(libs.plugins.detekt.plugin) apply false

    // Google Services - Using direct IDs instead of catalog for FOSS compatibility
    // These plugins are conditionally applied in app/build.gradle.kts for GMS builds only
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
