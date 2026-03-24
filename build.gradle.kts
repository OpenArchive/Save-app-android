plugins {
    // Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Kotlin
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false

    // Build Tools
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.androidx.room3) apply false

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

// FOSS Build Testing Tasks
// These tasks help test FOSS builds locally by mimicking F-Droid's prebuild steps

tasks.register("prepareFossBuild") {
    group = "build setup"
    description = "Prepare source for FOSS build (mimics F-Droid prebuild sed commands)"

    doLast {
        println("🔧 Removing Firebase/Crashlytics references...")
        println("   (Mimicking F-Droid prebuild sed commands)")
        println()

        fun runSed(pattern: String, file: String) {
            ProcessBuilder("sed", "-i", "", "-e", pattern, file)
                .directory(rootProject.projectDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        }

        // Version catalog modifications
        runSed("/^google-firebase-crashlytics = /d", "gradle/libs.versions.toml")
        runSed("/^google-gms-google-services = /d", "gradle/libs.versions.toml")
        runSed("/google-firebase-crashlytics = { id =/d", "gradle/libs.versions.toml")
        runSed("/google-gms-google-services = { id =/d", "gradle/libs.versions.toml")

        // Build file modifications
        runSed("/com.google.gms.google-services/d", "build.gradle.kts")
        runSed("/com.google.firebase.crashlytics/d", "build.gradle.kts")

        println("✅ FOSS build prepared. Files modified:")
        println("   - gradle/libs.versions.toml")
        println("   - build.gradle.kts")
        println()
        println("Next steps:")
        println("   1. Build: ./gradlew assembleFossProdRelease")
        println("   2. Restore: ./gradlew cleanupFossBuild")
    }
}

tasks.register("cleanupFossBuild") {
    group = "build setup"
    description = "Restore files after FOSS build (git checkout modified files)"

    doLast {
        println("🔄 Restoring modified files...")

        ProcessBuilder("git", "checkout", "gradle/libs.versions.toml", "build.gradle.kts")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
            .waitFor()

        println("✅ Files restored")
    }
}
