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
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.detekt.plugin)
    // Rust Android Gradle plugin - COMMENTED OUT due to Gradle 9.2 incompatibility
    // Use manual Rust build script instead (see rust-c2pa-ffi/build-android.sh)
    // id("org.mozilla.rust-android-gradle.rust-android") version "0.9.4"
    // Google Services plugins applied conditionally at bottom of file for GMS builds only
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

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "net.opendasharchive.openarchive"
        minSdk = 29
        targetSdk = 36
        versionCode = 30028
        versionName = "4.0.5"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
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
            isMinifyEnabled = false
            isShrinkResources = false
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
    }


    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk15to18:1.72")
            exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        }
    }
}

base {
    archivesName.set("save-${project.version}")
}

room {
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
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.recyclerview.selection)
    implementation(libs.androidx.viewpager2)
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

    // AndroidX Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment.compose)

    // AndroidX Navigation3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.androidx.navigationevent)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    //implementation(libs.androidx.compose.icons.extended)
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

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
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.guardianproject.sardine)
    implementation(libs.jsoup)

    // Images & Media
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.network)
    implementation(libs.picasso)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.extensions)

    // Barcode Scanning
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // Media3 - ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Google Play Services (GMS builds only)
    //implementation(libs.google.auth)
    //implementation(libs.google.play.asset.delivery.ktx)
    //implementation(libs.google.play.feature.delivery)
    //implementation(libs.google.play.feature.delivery.ktx)
    "gmsImplementation"(libs.google.play.review)
    "gmsImplementation"(libs.google.play.review.ktx)
    "gmsImplementation"(libs.google.play.app.update.ktx)
    "gmsImplementation"("com.google.android.gms:play-services-location:21.1.0")

    // Google Drive API
    //implementation(libs.google.api.client.android)
    //implementation(libs.google.http.client.gson)
    //implementation(libs.google.drive.api)

    // Security & Cryptography
    implementation("com.google.crypto.tink:tink-android:1.20.0")
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    api(libs.bouncycastle.bcpg)
    implementation(libs.netcipher)

    // Tor & Bitcoin
    implementation(libs.tor.android)
    implementation(libs.jtorctl)
    implementation(libs.bitcoinj.core)

    // C2PA - Content Authenticity
    // TODO: Add actual C2PA library once available
    // simple-c2pa (org.witness:simple-c2pa:0.0.13) is not available in Maven
    // Options:
    //   1. Use c2pa-android (https://github.com/contentauth/c2pa-android)
    //   2. Use c2pa-rs directly via JNI
    //   3. Build simple-c2pa from source
    // For now, using stub implementation in C2paHelper
    // implementation(libs.simple.c2pa)
    // implementation(libs.jna)

    // Barcode Scanning
    implementation(libs.google.mlkit.barcode)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // Utilities
    implementation(libs.timber)
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.guava.listenablefuture)
    implementation(libs.dotsindicator)
    implementation(libs.permissionx)
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

// ============================================================
// C2PA Rust FFI — Auto-build for FOSS variants
// ============================================================
//
// Task graph (runs only for FOSS builds):
//   mergeXxxJniLibFolders
//     └── buildC2paRustLibs  [incremental: skipped if .so files are up-to-date]
//           └── generateCargoConfig  [skipped if .cargo/config.toml already exists]
//                 └── installRustTargets  [idempotent rustup target add]
//                       └── restoreC2paSource  [skipped if Cargo.toml exists]
//
// To force a full rebuild:  ./gradlew buildC2paRustLibs --rerun-tasks
// To regenerate NDK config: delete rust-c2pa-ffi/.cargo/config.toml and rebuild

val preferredNdkVersion = "27.1.12297006"
val androidApiLevel = "30"
val rustC2paDir = rootProject.file("rust-c2pa-ffi")
val jniLibsDir = project.file("src/main/jniLibs")

// Rust target triple → Android ABI directory name
val rustToAbi = linkedMapOf(
    "aarch64-linux-android"   to "arm64-v8a",
    "armv7-linux-androideabi" to "armeabi-v7a",
    "i686-linux-android"      to "x86",
    "x86_64-linux-android"    to "x86_64"
)

fun findNdk(): File {
    // 1. Explicit NDK env var (highest priority)
    listOfNotNull(
        System.getenv("ANDROID_NDK_HOME"),
        System.getenv("ANDROID_NDK_ROOT"),
    ).map(::File).firstOrNull { it.isDirectory }?.let { return it }

    // 2. Find SDK, then look for ndk/ subdirectory
    val sdk = listOfNotNull(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        "${System.getProperty("user.home")}/Library/Android/sdk",  // macOS default
        "${System.getProperty("user.home")}/Android/Sdk",          // Linux/Windows default
    ).map(::File).firstOrNull { it.isDirectory }
        ?: throw GradleException(
            "Android SDK not found.\n" +
            "Set ANDROID_HOME to your SDK root directory and try again."
        )

    val ndkParent = File(sdk, "ndk")
    if (!ndkParent.isDirectory) throw GradleException(
        "Android NDK not found at $ndkParent.\n" +
        "Install it via Android Studio → SDK Manager → SDK Tools → NDK (Side by side)."
    )

    // Prefer the pinned version; otherwise use the latest installed
    File(ndkParent, preferredNdkVersion).takeIf { it.isDirectory }?.let { return it }
    return ndkParent.listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?: throw GradleException("No NDK versions found in $ndkParent")
}

fun ndkBinDir(ndk: File): File {
    val prebuilt = File(ndk, "toolchains/llvm/prebuilt")
    return prebuilt.listFiles()
        ?.firstOrNull { it.isDirectory }
        ?.let { File(it, "bin") }
        ?: throw GradleException("NDK prebuilt toolchain not found under $prebuilt")
}

/** Runs a command via ProcessBuilder, streams output to stdout/stderr, throws on non-zero exit. */
fun runCommand(vararg cmd: String, workDir: File = rootProject.projectDir, env: Map<String, String> = emptyMap()) {
    val pb = ProcessBuilder(*cmd).directory(workDir).inheritIO()
    if (env.isNotEmpty()) pb.environment().putAll(env)
    val result = pb.start().waitFor()
    check(result == 0) { "Command failed (exit $result): ${cmd.joinToString(" ")}" }
}

// Maps each Rust triple to its (clang binary prefix, CC env-var key)
val targetDetails = linkedMapOf(
    "aarch64-linux-android"   to Pair("aarch64-linux-android${androidApiLevel}",   "aarch64_linux_android"),
    "armv7-linux-androideabi" to Pair("armv7a-linux-androideabi${androidApiLevel}", "armv7_linux_androideabi"),
    "i686-linux-android"      to Pair("i686-linux-android${androidApiLevel}",       "i686_linux_android"),
    "x86_64-linux-android"    to Pair("x86_64-linux-android${androidApiLevel}",     "x86_64_linux_android"),
)

// ─── Task 1: Restore source from git if the directory was deleted ────────────
val restoreC2paSource by tasks.registering {
    group = "c2pa"
    description = "Restores rust-c2pa-ffi/ from git HEAD if the directory is missing"
    onlyIf { !File(rustC2paDir, "Cargo.toml").exists() }
    doLast {
        logger.lifecycle("rust-c2pa-ffi/ missing — restoring from git HEAD...")
        runCommand("git", "checkout", "HEAD", "--", "rust-c2pa-ffi")
        logger.lifecycle("✓ rust-c2pa-ffi/ restored from git")
    }
}

// ─── Task 2: Validate Rust toolchain; auto-install Android targets ───────────
val installRustTargets by tasks.registering {
    group = "c2pa"
    description = "Verifies Cargo is installed and ensures all Android Rust targets are added"
    dependsOn(restoreC2paSource)
    doLast {
        val cargoOk = ProcessBuilder("cargo", "--version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
        if (!cargoOk) throw GradleException(
            "Cargo not found. Install Rust from https://rustup.rs/\n" +
            "Then run: rustup target add ${rustToAbi.keys.joinToString(" ")}"
        )
        // rustup target add is idempotent — safe to call on every build
        runCommand(*( listOf("rustup", "target", "add") + rustToAbi.keys ).toTypedArray())
        logger.lifecycle("✓ Rust Android targets verified")
    }
}

// ─── Task 3: Generate .cargo/config.toml using the installed NDK ─────────────
// Runs on every build but only writes to disk when content changes, so
// buildC2paRustLibs (which uses the file as input) stays UP-TO-DATE when NDK is unchanged.
val generateCargoConfig by tasks.registering {
    group = "c2pa"
    description = "Generates rust-c2pa-ffi/.cargo/config.toml with NDK linker paths"
    dependsOn(installRustTargets)
    doLast {
        val ndk = findNdk()
        val bin = ndkBinDir(ndk)

        // NOTE: CC_*/AR_* env vars must live in the top-level [env] section.
        // [target.X.env] is NOT a valid Cargo config key and is silently ignored,
        // which causes cc-rs (used by the ring crate) to fail with "tool not found".
        val newContent = buildString {
            appendLine("# Auto-generated by Gradle — do not edit manually.")
            appendLine("# Regenerate: run ./gradlew generateCargoConfig --rerun-tasks")
            appendLine("# NDK: ${ndk.absolutePath}")
            appendLine()
            appendLine("[env]")
            appendLine("ANDROID_NDK_HOME = \"${ndk.absolutePath}\"")
            // CC/CXX/AR vars for cc-rs and other build scripts (one entry per ABI, all in [env])
            for ((_, pair) in targetDetails) {
                val (clangPrefix, envKey) = pair
                appendLine("CC_$envKey  = \"$bin/${clangPrefix}-clang\"")
                appendLine("CXX_$envKey = \"$bin/${clangPrefix}-clang++\"")
                appendLine("AR_$envKey  = \"$bin/llvm-ar\"")
            }
            appendLine()
            for ((triple, pair) in targetDetails) {
                val (clangPrefix, _) = pair
                appendLine("[target.$triple]")
                appendLine("linker = \"$bin/${clangPrefix}-clang\"")
                appendLine("ar = \"$bin/llvm-ar\"")
                appendLine("rustflags = [\"-C\", \"link-arg=-Wl,-z,max-page-size=16384\"]")
                appendLine()
            }
            appendLine("[build]")
            appendLine("target-dir = \"target\"")
        }

        val configFile = File(rustC2paDir, ".cargo/config.toml")
        File(rustC2paDir, ".cargo").mkdirs()
        if (!configFile.exists() || configFile.readText() != newContent) {
            configFile.writeText(newContent)
            logger.lifecycle("✓ .cargo/config.toml written (NDK: ${ndk.absolutePath})")
        } else {
            logger.lifecycle("✓ .cargo/config.toml unchanged")
        }
    }
}

// ─── Task 4: Compile the Rust library for all Android ABIs ───────────────────
val buildC2paRustLibs by tasks.registering {
    group = "c2pa"
    description = "Compiles libc2pa_ffi.so for all Android ABIs (incremental)"
    dependsOn(generateCargoConfig)

    // Gradle skips this task automatically when inputs are unchanged and outputs exist
    inputs.dir(File(rustC2paDir, "src"))
    inputs.files(
        File(rustC2paDir, "Cargo.toml"),
        File(rustC2paDir, "Cargo.lock"),
        File(rustC2paDir, ".cargo/config.toml"),
    )
    outputs.files(rustToAbi.values.map { abi -> jniLibsDir.resolve("$abi/libc2pa_ffi.so") })

    doLast {
        val bin = ndkBinDir(findNdk())
        rustToAbi.forEach { (rustTarget, abi) ->
            logger.lifecycle("  Building $abi ($rustTarget)...")
            val (clangPrefix, envKey) = targetDetails[rustTarget]!!
            // Pass CC/AR explicitly — belt-and-suspenders alongside .cargo/config.toml [env]
            val env = mapOf(
                "CC_$envKey"  to "$bin/${clangPrefix}-clang",
                "CXX_$envKey" to "$bin/${clangPrefix}-clang++",
                "AR_$envKey"  to "$bin/llvm-ar",
            )
            runCommand("cargo", "build", "--target", rustTarget, "--release", workDir = rustC2paDir, env = env)
            val soFile = rustC2paDir.resolve("target/$rustTarget/release/libc2pa_ffi.so")
            check(soFile.exists()) {
                "cargo build succeeded but .so not found at: ${soFile.absolutePath}"
            }
            jniLibsDir.resolve(abi).mkdirs()
            soFile.copyTo(jniLibsDir.resolve("$abi/libc2pa_ffi.so"), overwrite = true)
            logger.lifecycle("  ✓ $abi/libc2pa_ffi.so (${soFile.length() / 1024} KB)")
        }
        logger.lifecycle("C2PA Rust FFI build complete.")
    }
}

// ─── Wire buildC2paRustLibs into ALL variant builds (GMS + FOSS) ─────────────
afterEvaluate {
    tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
        .configureEach { dependsOn(buildC2paRustLibs) }
}

// Conditionally apply Google Services plugins only for GMS builds
if (gradle.startParameter.taskRequests.toString().contains("Gms", ignoreCase = true)) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
