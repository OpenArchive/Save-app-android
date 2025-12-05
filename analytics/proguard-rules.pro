# Add project specific ProGuard rules here.

# Keep analytics API
-keep public class net.opendasharchive.openarchive.analytics.api.** { *; }
-keep public interface net.opendasharchive.openarchive.analytics.api.** { *; }

# Keep event classes (used via reflection by providers)
-keep class net.opendasharchive.openarchive.analytics.api.AnalyticsEvent$** { *; }

# Mixpanel
-dontwarn com.mixpanel.**
-keep class com.mixpanel.android.** { *; }

# CleanInsights
-keep class org.cleaninsights.sdk.** { *; }

# Firebase
-keep class com.google.firebase.analytics.** { *; }
