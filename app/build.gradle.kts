import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.detekt.plugin)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
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

android {

    //noinspection GradleDependency
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "net.opendasharchive.openarchive"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 30006
        versionName = "0.7.8"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = loadLocalProperties()
        resValue("string", "mixpanel_key", localProps.getProperty("MIXPANELKEY") ?: "")
    }

    base {
        archivesName.set("save-${project.version}")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    signingConfigs {
        create("release") {
            val props = loadLocalProperties()
            storeFile = file(props["STOREFILE"] as? String ?: "")
            storePassword = props["STOREPASSWORD"] as? String ?: ""
            keyAlias = props["KEYALIAS"] as? String ?: ""
            keyPassword = props["KEYPASSWORD"] as? String ?: ""
        }

        getByName("debug") {
            val props = loadLocalProperties()
            storeFile = file(props["STOREFILE"] as? String ?: "")
            storePassword = props["STOREPASSWORD"] as? String ?: ""
            keyAlias = props["KEYALIAS"] as? String ?: ""
            keyPassword = props["KEYPASSWORD"] as? String ?: ""
        }
    }

    buildTypes {

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".release"
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            buildConfigField("Boolean", "ENABLE_BUILD_CACHE", "true")
        }
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/LICENSE.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE",
                    "META-INF/NOTICE", "META-INF/DEPENDENCIES", "LICENSE.txt"
                )
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

    namespace = "net.opendasharchive.openarchive"

    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk15to18:1.72")
            exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        }
    }
}

dependencies {

    // Core Kotlin and Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)


    // AndroidX Libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview.selection)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)

    // Compose Preferences
    implementation(libs.compose.preferences)

    // Material Design
    implementation(libs.google.material)

    // AndroidX SwipeRefreshLayout
    implementation(libs.androidx.swiperefresh)

    // Compose Libraries
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.fragment.compose)

    // Preference
    implementation(libs.androidx.preferences)

    // Dependency Injection
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.navigation)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    // Image Libraries
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.asafirm.image.picker)
    implementation(libs.picasso)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Networking and Data
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.guardianproject.sardine)

    // Utility Libraries
    implementation(libs.timber)
    implementation(libs.orhanobut.logger)
    implementation(libs.abdularis.circularimageview)
    implementation(libs.dotsindicator)
    implementation(libs.permissionx)

    // Barcode Scanning
    //implementation("com.google.zxing:core:3.5.3")
    //implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Security and Encryption
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.bouncycastle.bcprov)
    api(libs.bouncycastle.bcpg)

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation(libs.google.play.asset.delivery.ktx)
    implementation(libs.google.play.feature.delivery)
    implementation(libs.google.play.feature.delivery.ktx)
    implementation(libs.google.play.review)
    implementation(libs.google.play.review.ktx)
    implementation(libs.google.play.app.update.ktx)

    // Google Drive API
    implementation("com.google.http-client:google-http-client-gson:1.42.3")
    implementation("com.google.api-client:google-api-client-android:1.26.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // Tor Libraries
    implementation(libs.tor.android)
    implementation(libs.jtorctl)

    implementation(libs.bitcoinj.core)
    implementation("com.eclipsesource.j2v8:j2v8:6.2.1@aar")

    // ProofMode //from here: https://github.com/guardianproject/proofmode
    implementation(libs.proofmode) {
        //transitive = false
        exclude(group = "org.bitcoinj")
        exclude(group = "com.google.protobuf")
        exclude(group = "org.slf4j")
        exclude(group = "net.jcip")
        exclude(group = "commons-cli")
        exclude(group = "org.json")
        exclude(group = "com.google.guava")
        exclude(group = "com.google.guava", module = "guava-jdk5")
        exclude(group = "com.google.code.findbugs", module = "annotations")
        exclude(group = "com.squareup.okio", module = "okio")
    }

    // Guava Conflicts
    implementation(libs.guava)
    implementation(libs.guava.listenablefuture)

    implementation(libs.satyan.sugar)

    // adding web dav support: https://github.com/thegrizzlylabs/sardine-android'
    implementation("com.github.guardianproject:sardine-android:89f7eae512")

    implementation("com.github.derlio:audio-waveform:v1.0.1")

    implementation(libs.clean.insights)
    implementation(libs.netcipher)

    // Mixpanel analytics
    implementation(libs.mixpanel)

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    testImplementation("androidx.work:work-testing:2.9.1")

    // Detekt
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.rules.authors)
    detektPlugins(libs.detekt.rules.libraries)
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.rules.compose)

//    debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
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