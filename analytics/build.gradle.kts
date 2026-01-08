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

    // Match parent app's flavor dimensions
    flavorDimensions += listOf("distribution", "env")

    productFlavors {
        create("gms") {
            dimension = "distribution"
        }

        create("foss") {
            dimension = "distribution"
        }

        create("dev") {
            dimension = "env"
        }

        create("staging") {
            dimension = "env"
        }

        create("prod") {
            dimension = "env"
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

    // Analytics SDKs - flavor specific
    "gmsApi"(libs.mixpanel)
    "gmsApi"(libs.firebase.analytics)

    // CleanInsights for both GMS and FOSS builds
    api(libs.clean.insights)

    // Crash Reporting - flavor specific
    "gmsApi"(libs.firebase.crashlytics)
    "fossApi"("ch.acra:acra-http:5.11.3")
    "fossApi"("ch.acra:acra-dialog:5.11.3")

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
