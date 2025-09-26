plugins {
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.detekt.plugin) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// Properties needed by ProofMode submodule
ext {
    set("supportLibVersion", "28.0.0")
    set("buildToolsVersion", "34.0.0")
    set("androidxAnnotationVersion", "1.5.0")
    set("guavaVersion", "31.1-android")
    set("coreVersion", "1.5.0")
    set("extJUnitVersion", "1.1.4")
    set("runnerVersion", "1.5.0")
    set("rulesVersion", "1.5.0")
    set("espressoVersion", "3.5.0")
    set("truthVersion", "1.1.3")
    set("compileSdkVersion", 35)
    set("minSdkVersion", 28)
    set("targetSdkVersion", 35)
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
