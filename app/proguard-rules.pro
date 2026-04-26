# ============================================================
# OkHttp / Okio
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================================
# AndroidX Credentials
# ============================================================
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# ============================================================
# Room — keep entity/DAO class names for SQLite reflection
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration

# ============================================================
# Koin — DSL-based DI, no annotations, but uses class names at runtime
# ============================================================
-keep class org.koin.** { *; }
-keepnames class * { @org.koin.core.annotation.* *; }

# ============================================================
# Retrofit + kotlinx.serialization
# ============================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class net.opendasharchive.openarchive.**$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# ============================================================
# Gson — only used for Sugar ORM legacy models
# Remove once Sugar → Room migration complete
# ============================================================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# Sugar ORM — keep until Room migration complete
# ============================================================
-keep class com.orm.** { *; }
-keep class * extends com.orm.SugarRecord { *; }
-keepclassmembers class * extends com.orm.SugarRecord { *; }

# ============================================================
# Tor
# ============================================================
-keep class net.freehaven.tor.control.** { *; }
-keep class org.torproject.jni.** { *; }
-dontwarn org.torproject.**

# ============================================================
# Snowbird / C2PA native JNI bridges
# ============================================================
-keep class net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge { *; }
-keep class net.opendasharchive.openarchive.util.C2paFfi { *; }

# ============================================================
# App — keep JNI-callable methods and Parcelables
# ============================================================
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Compose — keep stability annotations
# ============================================================
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# Coil
# ============================================================
-dontwarn coil.**

# ============================================================
# Timber
# ============================================================
-dontwarn org.jetbrains.annotations.**

# ============================================================
# Kotlin reflect (used by Koin + serialization)
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================================
# CameraX
# ============================================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ============================================================
# ZXing
# ============================================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# ============================================================
# WorkManager
# ============================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker { public <init>(...); }
-keepclassmembers class * extends androidx.work.CoroutineWorker { public <init>(...); }

# ============================================================
# CleanInsights SDK — uses Moshi reflection to deserialize Configuration subclass.
# R8 must not rename or strip any CleanInsights classes or their members.
# ============================================================
-keep class org.cleaninsights.sdk.** { *; }
-keepclassmembers class org.cleaninsights.sdk.** { *; }

# Moshi — keep all JsonClass-annotated classes and their adapters
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# ============================================================
# xpp3 / XmlPullParser — Android SDK provides this natively;
# the xpp3 jar ships a duplicate that confuses R8. Suppress.
# ============================================================
-dontwarn org.xmlpull.v1.**
-dontwarn org.xmlpull.**
-keep class org.xmlpull.v1.** { *; }

# ============================================================
# DataStore
# ============================================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# ============================================================
# MetadataCollector — Class.forName() on these two classes at runtime.
# R8 must not rename them or ClassNotFoundException is thrown.
# ============================================================
-keep class net.opendasharchive.openarchive.util.GmsLocationProvider { *; }
-keep class net.opendasharchive.openarchive.util.FossLocationProvider { *; }

# ============================================================
# Sugar ORM model subclasses — field names mapped to DB columns via reflection.
# Renaming any field breaks reads silently (returns null) or crashes.
# Remove this block once Sugar → Room migration is complete.
# ============================================================
-keep class net.opendasharchive.openarchive.db.sugar.** { *; }
-keepclassmembers class net.opendasharchive.openarchive.db.sugar.** { *; }

# ============================================================
# Snowbird DTO serializers — kotlinx.serialization generates $$serializer
# companions that R8 strips if not explicitly kept.
# ============================================================
-keep,includedescriptorclasses class net.opendasharchive.openarchive.services.snowbird.data.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class net.opendasharchive.openarchive.services.snowbird.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# Koin type resolution — Koin matches types by KClass name at runtime.
# Renaming app classes breaks any of the 35 by inject() / get() call sites.
# ============================================================
-keepnames class net.opendasharchive.openarchive.** { *; }

# ============================================================
# Sardine (WebDAV) — parses XML responses by mapping element names to
# fields via reflection. Renaming fields produces silent null reads.
# ============================================================
-dontwarn org.apache.**
-keep class org.apache.** { *; }
-dontwarn com.github.sardine.**
-keep class com.github.sardine.** { *; }
-dontwarn com.thegrizzlylabs.sardine.**
-keep class com.thegrizzlylabs.sardine.** { *; }
-dontwarn info.guardianproject.sardine.**
-keep class info.guardianproject.sardine.** { *; }
