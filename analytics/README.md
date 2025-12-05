# Analytics Module

Unified analytics tracking module for the Save Android app with GDPR-compliant multi-provider support.

## Features

- **Multi-Provider Architecture**: CleanInsights (privacy-focused), Mixpanel (detailed analytics), and Firebase Analytics
- **Automatic PII Sanitization**: GDPR-compliant - automatically removes file paths, URLs, emails, IP addresses, tokens
- **Modern Kotlin**: Coroutines, StateFlow, sealed interfaces
- **Dependency Injection**: Koin-based DI for easy testing and modularity
- **Type-Safe Events**: 30+ predefined event types with compile-time safety
- **Session Tracking**: Reactive session management with upload statistics
- **Debug Mode Support**: Configurable debug flag for Firebase DebugView testing

## Installation

### 1. Add Module Dependency

In your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":analytics"))
}
```

### 2. Initialize Koin Module

In your `Application` class:

```kotlin
class SaveApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SaveApp)
            modules(
                // ... other modules
                analyticsModule(
                    mixpanelToken = getString(R.string.mixpanel_key),
                    cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
                )
            )
        }

        // Initialize analytics asynchronously
        val analyticsManager: AnalyticsManager by inject()
        lifecycleScope.launch {
            analyticsManager.initialize(this@SaveApp)
            analyticsManager.setUserProperty("app_version", BuildConfig.VERSION_NAME)
            analyticsManager.setUserProperty("device_type", "android")
        }
    }
}
```

### 3. Add CleanInsights Configuration

Place your `cleaninsights.json` file in `/analytics/src/main/assets/cleaninsights.json`.

## Usage

### Basic Event Tracking

```kotlin
class MainActivity : AppCompatActivity() {
    private val analyticsManager: AnalyticsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            analyticsManager.trackEvent(
                AnalyticsEvent.ScreenViewed(
                    screenName = "MainActivity",
                    previousScreen = "LaunchScreen"
                )
            )
        }
    }
}
```

### Session Tracking

```kotlin
class SaveApp : Application(), DefaultLifecycleObserver {
    private val sessionTracker: SessionTracker by inject()

    override fun onStart(owner: LifecycleOwner) {
        lifecycleScope.launch {
            sessionTracker.startSession()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleScope.launch {
            sessionTracker.endSession()
        }
    }
}
```

### Convenience Methods

```kotlin
lifecycleScope.launch {
    // Screen tracking
    analyticsManager.trackScreenView("SettingsScreen")

    // Upload tracking
    analyticsManager.trackUploadStarted("Internet Archive", "image", 2048)
    analyticsManager.trackUploadCompleted(
        backendType = "Internet Archive",
        fileType = "image",
        fileSizeKB = 2048,
        durationSeconds = 12,
        uploadSpeedKBps = 170
    )

    // Feature toggles
    analyticsManager.trackFeatureToggled("dark_mode", enabled = true)

    // Error tracking
    analyticsManager.trackError("network", "UploadScreen", "Internet Archive")
}
```

## Available Events

### App Lifecycle
- `AppOpened` - App launch with first-launch detection
- `AppClosed` - App closure with session duration
- `AppBackgrounded` - App moved to background
- `AppForegrounded` - App moved to foreground

### Screen Tracking
- `ScreenViewed` - Screen display with time spent
- `NavigationAction` - Navigation between screens

### Backend Usage
- `BackendConfigured` - Backend setup/update
- `BackendRemoved` - Backend removal

### Upload Metrics
- `UploadStarted` - Upload initiated
- `UploadCompleted` - Upload successful
- `UploadFailed` - Upload failed
- `UploadCancelled` - User cancelled upload
- `UploadSessionStarted` - Upload session started (1+ files)
- `UploadSessionCompleted` - Upload session finished
- `UploadNetworkError` - Network-specific errors

### Media Actions
- `MediaCaptured` - Photo/video captured
- `MediaSelected` - Media selected from gallery
- `MediaDeleted` - Media deleted

### Feature Usage
- `FeatureToggled` - Feature enabled/disabled

### Error Tracking
- `ErrorOccurred` - Error with category and screen context

### Session Tracking
- `SessionStarted` - Session begin
- `SessionEnded` - Session end with upload stats

### Engagement
- `ReviewPromptShown` - In-app review prompt displayed
- `ReviewPromptCompleted` - Review submitted
- `ReviewPromptError` - Review error occurred

## Architecture

```
AnalyticsManager (Interface)
       ↓
AnalyticsManagerImpl
       ↓
   ┌───┼───┐
   ↓   ↓   ↓
 CleanInsights  Mixpanel  Firebase
 Provider       Provider  Provider
   ↓   ↓   ↓
 SDKs (runs in parallel)
```

**Key Design Patterns:**
- **Facade Pattern**: `AnalyticsManager` provides unified interface
- **Strategy Pattern**: `AnalyticsProvider` interface for pluggable providers
- **Repository Pattern**: Provider isolation with error handling
- **Observer Pattern**: StateFlow for reactive session state

## Firebase Analytics Verification

### 1. Enable Debug Mode

```bash
adb shell setprop debug.firebase.analytics.app net.opendasharchive.openarchive.debug
```

### 2. Open Firebase Console

Navigate to: Firebase Console → DebugView

### 3. Verify Events

Check that events appear with:
- Event names <= 40 characters
- Parameter names <= 40 characters
- Parameter values <= 100 characters
- No PII (file paths, URLs, emails removed)

### 4. Debug Build Configuration

The module includes a `ENABLE_ANALYTICS_IN_DEBUG` flag:
- **DEBUG builds**: Analytics enabled by default for testing
- **RELEASE builds**: Analytics enabled

To disable analytics in DEBUG builds, modify `/analytics/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("boolean", "ENABLE_ANALYTICS_IN_DEBUG", "false")
    }
}
```

## GDPR Compliance

All events are automatically sanitized before being sent to providers:

**What gets redacted:**
- File paths → `[FILE_PATH]`
- URLs → `[URL]`
- Emails → `[EMAIL]`
- IP addresses → `[IP_ADDRESS]`
- Usernames → `user=[REDACTED]`
- Passwords → `password=[REDACTED]`
- Tokens/Keys → `token=[REDACTED]`

**Example:**
```kotlin
// Input: "Upload failed: /storage/emulated/0/DCIM/photo.jpg to https://example.com"
// Output: "Upload failed: [FILE_PATH] to [URL]"
```

## Testing

### Unit Testing

```kotlin
class AnalyticsTest {
    private lateinit var manager: AnalyticsManager
    private lateinit var fakeProviders: List<FakeAnalyticsProvider>

    @Before
    fun setup() {
        fakeProviders = listOf(FakeAnalyticsProvider())
        manager = AnalyticsManagerImpl(fakeProviders)
    }

    @Test
    fun `trackEvent dispatches to all providers`() = runTest {
        val event = AnalyticsEvent.AppOpened(
            isFirstLaunch = true,
            appVersion = "1.0.0"
        )

        manager.trackEvent(event)

        assert(fakeProviders[0].trackedEvents.contains(event))
    }
}
```

## Module Structure

```
:analytics/
├── api/                    # Public API
│   ├── AnalyticsManager.kt
│   ├── AnalyticsEvent.kt
│   └── session/
│       ├── SessionTracker.kt
│       └── SessionTrackerImpl.kt
├── core/                   # Core abstractions
│   └── AnalyticsProvider.kt
├── providers/              # Provider implementations
│   ├── cleaninsights/
│   ├── mixpanel/
│   └── firebase/
├── util/                   # Utilities
│   └── PiiSanitizer.kt
└── di/                     # Dependency injection
    └── AnalyticsModule.kt
```

## Dependencies

```kotlin
// Kotlin Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

// AndroidX Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
implementation("androidx.lifecycle:lifecycle-process:2.10.0")

// Analytics SDKs
api("com.mixpanel.android:mixpanel-android:8.2.4")
api("org.cleaninsights.sdk:cleaninsights:2.8.0")
api("com.google.firebase:firebase-analytics:22.1.2")

// Dependency Injection
implementation("io.insert-koin:koin-core:4.2.0-alpha3")
implementation("io.insert-koin:koin-android:4.2.0-alpha3")
```

## Migration from Old Analytics

If migrating from the old object-based `AnalyticsManager`:

**Old Code:**
```kotlin
AnalyticsManager.trackScreenView("MainActivity")
SessionManager.setCurrentScreen("MainActivity")
```

**New Code:**
```kotlin
class MainActivity : AppCompatActivity() {
    private val analyticsManager: AnalyticsManager by inject()
    private val sessionTracker: SessionTracker by inject()

    override fun onResume() {
        lifecycleScope.launch {
            analyticsManager.trackScreenView("MainActivity")
        }
        sessionTracker.setCurrentScreen("MainActivity")
    }
}
```

## Troubleshooting

### Events not appearing in Firebase

1. Check DEBUG flag is enabled in `build.gradle.kts`
2. Verify Firebase DebugView is enabled: `adb shell setprop debug.firebase.analytics.app <package>`
3. Check `google-services.json` is in the app module
4. Ensure app is in foreground (Firebase batches events in background)

### CleanInsights not tracking

1. Verify user has granted consent: `CleanInsightsManager.hasConsent()`
2. Check `cleaninsights.json` exists in `/analytics/src/main/assets/`
3. Ensure campaign ID matches configuration

### Build errors

1. Ensure all old analytics imports are removed from app module
2. Clean build: `./gradlew clean :analytics:build`
3. Invalidate caches in Android Studio

## License

This module is part of the Save Android app.

## Support

For issues or questions, please file an issue on the project's GitHub repository.
