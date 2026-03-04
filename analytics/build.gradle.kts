plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

android {
    namespace = "net.opendasharchive.openarchive.analytics"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    // Match parent app's flavor dimensions (env only - no distribution split on this branch)
    flavorDimensions += listOf("env")

    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("boolean", "ENHANCED_ANALYTICS_ENABLED", "true")
        }

        create("staging") {
            dimension = "env"
            buildConfigField("boolean", "ENHANCED_ANALYTICS_ENABLED", "true")
        }

        create("prod") {
            dimension = "env"
            buildConfigField("boolean", "ENHANCED_ANALYTICS_ENABLED", "false")
        }
    }

buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_ANALYTICS_IN_DEBUG", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_ANALYTICS_IN_DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Kotlin
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Analytics SDKs
    api(libs.mixpanel)
    api(libs.mixpanel.session.replay)
    api(libs.firebase.analytics)
    api(libs.firebase.crashlytics)
    api(libs.clean.insights)

    // Logging
    implementation(libs.timber)

    // Dependency Injection
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // Testing (optional)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
