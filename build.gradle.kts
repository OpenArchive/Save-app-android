plugins {
    // Android
    alias(libs.plugins.android.application) apply false

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

    // Google Services
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
