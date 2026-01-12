package net.opendasharchive.openarchive.util

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility object for collecting device metadata and writing EXIF tags to image files.
 * Used for C2PA content provenance.
 */
object MetadataCollector {

    /**
     * Data class containing all captured metadata.
     */
    data class CaptureMetadata(
        val latitude: Double?,
        val longitude: Double?,
        val deviceMake: String,
        val deviceModel: String,
        val deviceBrand: String,
        val appName: String,
        val appVersion: String,
        val captureTime: Long
    )

    /**
     * Collect metadata for a captured photo.
     *
     * @param context Android context
     * @param includeLocation Whether to acquire GPS location (default: true)
     * @return CaptureMetadata with all collected information
     */
    suspend fun collectMetadata(
        context: Context,
        includeLocation: Boolean = true
    ): CaptureMetadata {
        // Only acquire location if explicitly requested AND permission granted
        val location = if (includeLocation) {
            withContext(Dispatchers.IO) {
                if (hasLocationPermission(context)) {
                    try {
                        getLocationProvider(context).getCurrentLocation()
                    } catch (e: Exception) {
                        AppLogger.e("[Metadata] Location acquisition failed", e)
                        null
                    }
                } else {
                    AppLogger.w("[Metadata] Location requested but permission not granted")
                    null
                }
            }
        } else {
            AppLogger.d("[Metadata] Location collection skipped (includeLocation=false)")
            null
        }

        return CaptureMetadata(
            latitude = location?.latitude,
            longitude = location?.longitude,
            deviceMake = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceBrand = Build.BRAND,
            appName = context.getString(R.string.app_name),
            appVersion = BuildConfig.VERSION_NAME,
            captureTime = System.currentTimeMillis()
        )
    }

    /**
     * Write EXIF metadata to an image file.
     *
     * @param file The image file to write metadata to
     * @param metadata The metadata to write
     * @throws Exception if EXIF writing fails
     */
    fun writeExifMetadata(file: File, metadata: CaptureMetadata) {
        try {
            val exif = ExifInterface(file.absolutePath)

            // Write location if available
            if (metadata.latitude != null && metadata.longitude != null) {
                exif.setLatLong(metadata.latitude, metadata.longitude)
                AppLogger.d("[Metadata] Wrote GPS: ${metadata.latitude}, ${metadata.longitude}")
            }

            // Write device info
            exif.setAttribute(ExifInterface.TAG_MAKE, metadata.deviceMake)
            exif.setAttribute(ExifInterface.TAG_MODEL, metadata.deviceModel)
            AppLogger.d("[Metadata] Wrote device: ${metadata.deviceMake} ${metadata.deviceModel}")

            // Write software info
            val software = "${metadata.appName} ${metadata.appVersion}"
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, software)
            AppLogger.d("[Metadata] Wrote software: $software")

            // Write timestamp
            val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val datetime = dateFormat.format(Date(metadata.captureTime))
            exif.setAttribute(ExifInterface.TAG_DATETIME, datetime)
            AppLogger.d("[Metadata] Wrote datetime: $datetime")

            exif.saveAttributes()
            AppLogger.d("[Metadata] EXIF metadata written successfully to ${file.name}")
        } catch (e: Exception) {
            AppLogger.e("[Metadata] Failed to write EXIF", e)
            throw e
        }
    }

    /**
     * Check if location permission is granted.
     *
     * @param context Android context
     * @return true if permission granted, false otherwise
     */
    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the appropriate LocationProvider based on build flavor.
     * Uses reflection to avoid compile-time dependencies on flavor-specific code.
     *
     * @param context Android context
     * @return LocationProvider implementation (GMS or FOSS)
     */
    private fun getLocationProvider(context: Context): LocationProvider {
        // Check if GMS location services are available
        return if (isGmsAvailable()) {
            try {
                // Try to instantiate GmsLocationProvider (only exists in GMS builds)
                Class.forName("net.opendasharchive.openarchive.util.GmsLocationProvider")
                    .getConstructor(Context::class.java)
                    .newInstance(context) as LocationProvider
            } catch (e: ClassNotFoundException) {
                AppLogger.w("[Metadata] GMS location class not found, falling back to FOSS provider")
                getFossLocationProvider(context)
            }
        } else {
            getFossLocationProvider(context)
        }
    }

    /**
     * Get FOSS location provider.
     */
    private fun getFossLocationProvider(context: Context): LocationProvider {
        return try {
            Class.forName("net.opendasharchive.openarchive.util.FossLocationProvider")
                .getConstructor(Context::class.java)
                .newInstance(context) as LocationProvider
        } catch (e: ClassNotFoundException) {
            AppLogger.e("[Metadata] No location provider available")
            throw IllegalStateException("No location provider implementation found", e)
        }
    }

    /**
     * Check if Google Mobile Services (GMS) location API is available.
     *
     * @return true if GMS is available, false otherwise
     */
    private fun isGmsAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.gms.location.FusedLocationProviderClient")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
