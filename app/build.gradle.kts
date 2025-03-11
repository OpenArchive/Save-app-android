import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.detekt.plugin)
}
android {

    val localPropsFile = file("../local.properties")
    val localProps = Properties()
    if (!localPropsFile.canRead()) {
        throw GradleException("Could not read local.properties!")
    }
    localProps.load(localPropsFile.inputStream())
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
        targetSdk = 34
        versionCode = 30006
        versionName = "0.7.8"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "mixpanel_key", localProps.getProperty("mixpanel.key") ?: "")
    }

    base {
        archivesName.set("save-${project.version}")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {

        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".release"
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    signingConfigs {
        getByName("debug") {
            val props = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { props.load(it) }
            }

            storeFile = file(props["storeFile"] as? String ?: "")
            storePassword = props["storePassword"] as? String ?: ""
            keyAlias = props["keyAlias"] as? String ?: ""
            keyPassword = props["keyPassword"] as? String ?: ""
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

    val composeVersion = "1.7.8"
    val activity = "1.9.3"
    val material = "1.12.0"
    val material3 = "1.3.1"
    val lifecycle = "2.8.7"
    val navigation = "2.8.8"
    val fragment = "1.8.6"
    val koin = "4.1.0-Beta5"

    val coil = "3.0.4"

    // Core Kotlin and Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")


    // AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle")


    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    implementation("androidx.fragment:fragment-ktx:$fragment")
    implementation("androidx.fragment:fragment-compose:$fragment")

    // Compose Preferences
    implementation("me.zhanghai.compose.preference:library:1.1.1")

    // Material Design
    implementation("com.google.android.material:material:$material")

    // AndroidX SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Compose Libraries
    implementation("androidx.activity:activity-ktx:$activity")
    implementation("androidx.activity:activity-compose:$activity")
    implementation("androidx.compose.material3:material3:$material3")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")

    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigation")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation")
    implementation("androidx.navigation:navigation-fragment-compose:$navigation")

    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koin")
    implementation("io.insert-koin:koin-android:$koin")
    implementation("io.insert-koin:koin-androidx-compose:$koin")
    implementation("io.insert-koin:koin-androidx-navigation:$koin")
    implementation("io.insert-koin:koin-compose:$koin")
    implementation("io.insert-koin:koin-compose-viewmodel:$koin")
    implementation("io.insert-koin:koin-compose-viewmodel-navigation:$koin")

    // Image Libraries
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.esafirm:android-image-picker:3.0.0")
    implementation("com.squareup.picasso:picasso:2.5.2")
    implementation("io.coil-kt.coil3:coil:$coil")
    implementation("io.coil-kt.coil3:coil-compose:$coil")
    implementation("io.coil-kt.coil3:coil-video:$coil")

    // Networking and Data
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.github.guardianproject:sardine-android:89f7eae512")

    // Utility Libraries
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.orhanobut:logger:2.2.0")
    implementation("com.github.abdularis:circularimageview:1.4")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation("com.guolindev.permissionx:permissionx:1.6.4")

    // Barcode Scanning
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.2.0")

    // Security and Encryption
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.72")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
    api("org.bouncycastle:bcpg-jdk15to18:1.71")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.3.0")
//    implementation("com.google.android.play:core-ktx:1.8.1")
//    implementation("com.google.android.play:asset-delivery-ktx:2.3.0")
//    implementation("com.google.android.play:feature-delivery-ktx:2.1.0")
//    implementation("com.google.android.play:review-ktx:2.0.2")
//    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Google Drive API
    implementation("com.google.http-client:google-http-client-gson:1.42.3")
    implementation("com.google.api-client:google-api-client-android:1.26.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // Tor Libraries
    implementation("info.guardianproject:tor-android:0.4.7.14")
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    implementation("org.bitcoinj:bitcoinj-core:0.16.2")
    implementation("com.eclipsesource.j2v8:j2v8:6.2.1@aar")

    // ProofMode //from here: https://github.com/guardianproject/proofmode
    implementation("org.proofmode:android-libproofmode:1.0.26") {
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
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")


    implementation("com.github.satyan:sugar:1.5")


    // adding web dav support: https://github.com/thegrizzlylabs/sardine-android'
    implementation("com.github.guardianproject:sardine-android:89f7eae512")


    implementation("com.github.derlio:audio-waveform:v1.0.1")


    implementation("org.cleaninsights.sdk:clean-insights-sdk:2.8.0")
    implementation("info.guardianproject.netcipher:netcipher:2.2.0-alpha")

    // Mixpanel analytics
    implementation("com.mixpanel.android:mixpanel-android:8.0.2")


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

/**
testdroid {username '$bbusername'
password '$bbpassword'
deviceGroup 'gpdevices'
mode "FULL_RUN"
projectName "OASave"}**/