import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.detekt.plugin)
    // Rust Android Gradle plugin - COMMENTED OUT due to Gradle 9.2 incompatibility
    // Use manual Rust build script instead (see rust-c2pa-ffi/build-android.sh)
    // id("org.mozilla.rust-android-gradle.rust-android") version "0.9.4"
    // Google Services plugins applied conditionally at bottom of file for GMS builds only
    // koin.compiler plugin REMOVED: only needed for annotation-based Koin (@Module/@Single).
    // This project uses DSL modules only, and the plugin caused spurious "Missing definition"
    // errors on incremental builds (whole-graph validation fails when not all files recompile).
}

fun loadLocalProperties(): Properties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        FileInputStream(localPropsFile).use { load(it) }
    } else {
        setProperty("MIXPANELKEY", System.getenv("MIXPANEL_KEY") ?: "")
        setProperty("STOREFILE", System.getenv("STOREFILE") ?: "")
        setProperty("STOREPASSWORD", System.getenv("STOREPASSWORD") ?: "")
        setProperty("KEYALIAS", System.getenv("KEYALIAS") ?: "")
        setProperty("KEYPASSWORD", System.getenv("KEYPASSWORD") ?: "")
    }
}

kotlin {
    compilerOptions {

        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)

        // ---- Experimental APIs ----
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api",)
        optIn.add("com.google.accompanist.permissions.ExperimentalPermissionsApi",)
        optIn.add("kotlin.time.ExperimentalTime",)
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi",)

        // ---- Kotlin compiler feature flags ----
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution",)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

android {

    namespace = "net.opendasharchive.openarchive"

    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "net.opendasharchive.openarchive"
        minSdk = 29
        targetSdk = 37
        versionCode = 30043
        versionName = "4.0.5"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        // minSdk=29 (Android 10) requires 64-bit hardware — all supported devices are arm64
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = loadLocalProperties()
        resValue("string", "mixpanel_key", localProps.getProperty("MIXPANELKEY") ?: "")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
        resValues = true
    }

    buildTypes {

        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    flavorDimensions += listOf("distribution", "env")

    productFlavors {

        // Distribution dimension
        create("gms") {
            isDefault = true
            dimension = "distribution"
            buildConfigField("boolean", "IS_GMS_BUILD", "true")
            buildConfigField("boolean", "IS_FOSS_BUILD", "false")
        }

        create("foss") {
            dimension = "distribution"
            // No applicationIdSuffix for FOSS builds
            // F-Droid expects: net.opendasharchive.openarchive.release
            buildConfigField("boolean", "IS_GMS_BUILD", "false")
            buildConfigField("boolean", "IS_FOSS_BUILD", "true")
            // ACRA crash report email - loaded from local.properties or env var
            val localProps = loadLocalProperties()
            val acraEmail = localProps.getProperty("ACRA_EMAIL") ?: System.getenv("ACRA_EMAIL") ?: ""
            buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
        }

        // Environment dimension
        create("dev") {
            isDefault = true
            dimension = "env"
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".debug"
        }

        create("staging") {
            dimension = "env"
            versionNameSuffix = "-staging"
            applicationIdSuffix = ".debug"
        }

        create("prod") {
            dimension = "env"
            applicationIdSuffix = ".release"
        }
    }

    signingConfigs {
        getByName("debug") {
            val props = loadLocalProperties()
            storeFile = file(props["STOREFILE"] as? String ?: "")
            storePassword = props["STOREPASSWORD"] as? String ?: ""
            keyAlias = props["KEYALIAS"] as? String ?: ""
            keyPassword = props["KEYPASSWORD"] as? String ?: ""
        }
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE.txt",
                    "META-INF/LICENSE",
                    "META-INF/NOTICE",
                    "META-INF/DEPENDENCIES",
                    "LICENSE.txt",
                ),
            )
        }
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    androidResources {
        generateLocaleConfig = true
        // Strip unused locale resources from all transitive libraries
        localeFilters += setOf(
            "en", "ar", "fa", "fr", "es", "de", "ckb", "pt", "ru", "zh", "tr", "id", "uk", "nl"
        )
    }


}

base {
    archivesName.set("save-${project.version}")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    // Kotlin Core
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)

    // AndroidX UI Components
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefresh)

    // AndroidX Activity & Fragment
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)

    // AndroidX Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // AndroidX Navigation (fragment kept for BaseFragment.findNavController)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.compose)

    // AndroidX Navigation3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigationevent)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.compose.preferences)
    implementation(libs.reorderable)
    implementation(libs.accompanist.permissions)

    // Material Design
    implementation(libs.google.material)

    // AndroidX Other
    implementation(libs.androidx.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work)

    // Room Database
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)

    // Dependency Injection - Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.navigation)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // Images & Media
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.network)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.extensions)

    // Barcode Scanning (ZXing only — ML Kit removed, 20 MB native saved)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // Media3 - ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Google Play Services (GMS builds only)
    "gmsImplementation"(libs.google.play.review)
    "gmsImplementation"(libs.google.play.review.ktx)
    "gmsImplementation"(libs.google.play.app.update.ktx)
    "gmsImplementation"("com.google.android.gms:play-services-location:21.1.0")
    "gmsImplementation"("com.google.android.play:integrity:1.4.0")

    // Tor
    implementation(libs.tor.android)
    implementation(libs.jtorctl)

    // C2PA - Content Authenticity (contentauth/c2pa-android)
    implementation("com.github.contentauth:c2pa-android:0.0.9")
    implementation("org.bouncycastle:bcprov-jdk18on:1.81")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")
    implementation("org.bouncycastle:bcpg-jdk18on:1.81")

    // Utilities
    implementation(libs.timber)
    implementation(libs.gson)
    implementation(libs.dotsindicator)
    implementation(libs.satyan.sugar)

    // Analytics Module (includes crash reporting)
    implementation(project(":analytics"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.work.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)

    // Detekt Plugins
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.rules.libraries)
    detektPlugins(libs.detekt.rules.authors)
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.rules.compose)
}

// xpp3 ships XmlPullParser which conflicts with Android's built-in version — exclude the jar
configurations.all {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "xpp3", module = "xpp3_min")
}

detekt {
    config.setFrom(file("$rootDir/config/detekt-config.yml"))
    baseline = file("$rootDir/config/baseline.xml")
    source.setFrom(
        files("$rootDir/app/src")
    )
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    ignoreFailures = true
}

// Conditionally apply Google Services plugins only for GMS builds
if (gradle.startParameter.taskRequests.toString().contains("Gms", ignoreCase = true)) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
