package net.opendasharchive.openarchive.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Utility object for collecting device metadata and writing EXIF tags to image files.
 * Field names mirror the ProofMode v1.0.25 schema for interoperability.
 */
object MetadataCollector {

    data class CaptureMetadata(
        // --- Location ---
        val latitude: Double?,
        val longitude: Double?,
        val locationAltitude: Double?,
        val locationAccuracy: Float?,
        val locationBearing: Float?,
        val locationSpeed: Float?,
        val locationProvider: String?,
        val locationTime: Long?,

        // --- Device ---
        val deviceMake: String,
        val deviceModel: String,
        val deviceBrand: String,
        val hardware: String,

        // --- App ---
        val appName: String,
        val appVersion: String,

        // --- Capture time ---
        val captureTime: Long,

        // --- Locale ---
        val locale: String,
        val language: String,

        // --- Screen ---
        val screenSizeInches: Double?,

        // --- Network (Phase 2 — ACCESS_NETWORK_STATE already in manifest) ---
        val networkType: String?,
        val ipv4: String?,
        val ipv6: String?,

        // --- Cell (Phase 3 — READ_PHONE_STATE, checked at runtime) ---
        val cellInfo: String?
    )

    /**
     * Collect all device/environment metadata. Location is fetched if the permission is
     * granted and [includeLocation] is true.
     */
    suspend fun collectMetadata(
        context: Context,
        includeLocation: Boolean = true
    ): CaptureMetadata {
        val location = if (includeLocation && hasLocationPermission(context)) {
            withContext(Dispatchers.IO) {
                try {
                    getLocationProvider(context).getCurrentLocation()
                } catch (e: Exception) {
                    AppLogger.e("[Metadata] Location acquisition failed", e)
                    null
                }
            }
        } else {
            if (includeLocation) AppLogger.w("[Metadata] Location permission not granted")
            null
        }

        return CaptureMetadata(
            // Location — extract every sub-field the Location object exposes
            latitude          = location?.latitude,
            longitude         = location?.longitude,
            locationAltitude  = if (location?.hasAltitude() == true) location.altitude  else null,
            locationAccuracy  = if (location?.hasAccuracy() == true) location.accuracy  else null,
            locationBearing   = if (location?.hasBearing()  == true) location.bearing   else null,
            locationSpeed     = if (location?.hasSpeed()    == true) location.speed     else null,
            locationProvider  = location?.provider,
            locationTime      = location?.time,

            // Device
            deviceMake  = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceBrand = Build.BRAND,
            hardware    = Build.HARDWARE,

            // App
            appName    = context.getString(R.string.app_name),
            appVersion = BuildConfig.VERSION_NAME,

            // Time
            captureTime = System.currentTimeMillis(),

            // Locale
            locale   = Locale.getDefault().country,
            language = Locale.getDefault().displayLanguage,

            // Screen
            screenSizeInches = getScreenSizeInches(context),

            // Network
            networkType = getNetworkType(context),
            ipv4        = getIpAddress(useIPv4 = true),
            ipv6        = getIpAddress(useIPv4 = false),

            // Cell
            cellInfo = getCellInfo(context)
        )
    }

    /**
     * Write EXIF metadata to an image file.
     * Only call this for image MIME types (JPEG, PNG, WebP, HEIC) — ExifInterface does not support video/audio.
     *
     * @param file  The image file to annotate (JPEG, PNG, WebP, HEIC supported by ExifInterface)
     * @param metadata  Metadata collected via [collectMetadata]
     * @throws Exception if EXIF writing fails
     */
    fun writeExifMetadata(file: File, metadata: CaptureMetadata) {
        try {
            val exif = ExifInterface(file.absolutePath)

            // --- GPS coordinates ---
            if (metadata.latitude != null && metadata.longitude != null) {
                exif.setLatLong(metadata.latitude, metadata.longitude)
                AppLogger.d("[Metadata] EXIF GPS: ${metadata.latitude}, ${metadata.longitude}")
            }

            // --- GPS altitude ---
            if (metadata.locationAltitude != null) {
                // ExifInterface expects the absolute value; ref "0"=above, "1"=below sea level
                exif.setAttribute(
                    ExifInterface.TAG_GPS_ALTITUDE,
                    abs(metadata.locationAltitude).toBigDecimal().toPlainString()
                )
                exif.setAttribute(
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    if (metadata.locationAltitude < 0) "1" else "0"
                )
            }

            // --- GPS speed (EXIF uses km/h; Location gives m/s) ---
            if (metadata.locationSpeed != null) {
                val speedKmh = metadata.locationSpeed * 3.6
                exif.setAttribute(ExifInterface.TAG_GPS_SPEED, speedKmh.toBigDecimal().toPlainString())
                exif.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, "K") // K = km/h
            }

            // --- GPS bearing/track direction ---
            if (metadata.locationBearing != null) {
                exif.setAttribute(
                    ExifInterface.TAG_GPS_TRACK,
                    metadata.locationBearing.toBigDecimal().toPlainString()
                )
                exif.setAttribute(ExifInterface.TAG_GPS_TRACK_REF, "T") // T = True north
            }

            // --- GPS fix timestamp (separate from capture time) ---
            if (metadata.locationTime != null) {
                val gpsDate = Date(metadata.locationTime)
                val timeDf  = SimpleDateFormat("HH:mm:ss", Locale.US).also {
                    it.timeZone = TimeZone.getTimeZone("UTC")
                }
                val dateDf  = SimpleDateFormat("yyyy:MM:dd", Locale.US).also {
                    it.timeZone = TimeZone.getTimeZone("UTC")
                }
                exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, timeDf.format(gpsDate))
                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateDf.format(gpsDate))
            }

            // --- GPS provider (stored in ProcessingMethod tag) ---
            if (metadata.locationProvider != null) {
                // Format: "charset=Ascii GPS" or "charset=Ascii NETWORK" etc.
                exif.setAttribute(
                    ExifInterface.TAG_GPS_PROCESSING_METHOD,
                    "charset=Ascii ${metadata.locationProvider.uppercase()}"
                )
            }

            // --- Device make/model ---
            exif.setAttribute(ExifInterface.TAG_MAKE, metadata.deviceMake)
            exif.setAttribute(ExifInterface.TAG_MODEL, metadata.deviceModel)

            // --- Software (app name + version) ---
            exif.setAttribute(
                ExifInterface.TAG_SOFTWARE,
                "${metadata.appName} ${metadata.appVersion}"
            )

            // --- Capture datetime ---
            val dtFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            exif.setAttribute(ExifInterface.TAG_DATETIME, dtFormat.format(Date(metadata.captureTime)))

            exif.saveAttributes()
            AppLogger.d("[Metadata] EXIF written to ${file.name}")
        } catch (e: Exception) {
            AppLogger.e("[Metadata] Failed to write EXIF to ${file.name}", e)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun getLocationProvider(context: Context): LocationProvider {
        return if (isGmsAvailable()) {
            try {
                @Suppress("UNCHECKED_CAST")
                Class.forName("net.opendasharchive.openarchive.util.GmsLocationProvider")
                    .getConstructor(Context::class.java)
                    .newInstance(context) as LocationProvider
            } catch (e: ClassNotFoundException) {
                AppLogger.w("[Metadata] GMS location class not found, falling back to FOSS")
                getFossLocationProvider(context)
            }
        } else {
            getFossLocationProvider(context)
        }
    }

    private fun getFossLocationProvider(context: Context): LocationProvider {
        return try {
            @Suppress("UNCHECKED_CAST")
            Class.forName("net.opendasharchive.openarchive.util.FossLocationProvider")
                .getConstructor(Context::class.java)
                .newInstance(context) as LocationProvider
        } catch (e: ClassNotFoundException) {
            AppLogger.e("[Metadata] No location provider found")
            throw IllegalStateException("No location provider implementation found", e)
        }
    }

    private fun isGmsAvailable(): Boolean = try {
        Class.forName("com.google.android.gms.location.FusedLocationProviderClient")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    /** Diagonal screen size in inches, derived from DisplayMetrics. */
    private fun getScreenSizeInches(context: Context): Double? = try {
        val dm = context.resources.displayMetrics
        val w  = dm.widthPixels  / dm.xdpi.toDouble()
        val h  = dm.heightPixels / dm.ydpi.toDouble()
        sqrt(w * w + h * h)
    } catch (e: Exception) {
        AppLogger.w("[Metadata] Screen size unavailable: ${e.message}")
        null
    }

    /** Active network transport type. Requires ACCESS_NETWORK_STATE (already in manifest). */
    private fun getNetworkType(context: Context): String? {
        return try {
            val cm      = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "NONE"
            val caps    = cm.getNetworkCapabilities(network) ?: return null
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else                                                       -> "OTHER"
            }
        } catch (e: Exception) {
            AppLogger.w("[Metadata] Network type unavailable: ${e.message}")
            null
        }
    }

    /** First non-loopback IPv4 or IPv6 address from any active interface. */
    private fun getIpAddress(useIPv4: Boolean): String? = try {
        NetworkInterface.getNetworkInterfaces()
            ?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { addr ->
                !addr.isLoopbackAddress &&
                if (useIPv4) addr is Inet4Address else addr is Inet6Address
            }
            ?.hostAddress
    } catch (e: Exception) {
        AppLogger.w("[Metadata] IP address (ipv4=$useIPv4) unavailable: ${e.message}")
        null
    }

    /**
     * Cellular neighbour cell info as a JSON array matching the ProofMode CellInfo format.
     * Returns null if READ_PHONE_STATE is not granted.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getCellInfo(context: Context): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return null

        return try {
            val tm    = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cells = tm.allCellInfo ?: return null

            val entries = cells.mapNotNull { cell ->
                when (cell) {
                    is CellInfoLte   -> mapOf(
                        "cellId" to cell.cellIdentity.ci,
                        "tac"    to cell.cellIdentity.tac,
                        "dbm"    to cell.cellSignalStrength.dbm
                    )
                    is CellInfoGsm   -> mapOf(
                        "cellId" to cell.cellIdentity.cid,
                        "lac"    to cell.cellIdentity.lac,
                        "dbm"    to cell.cellSignalStrength.dbm
                    )
                    is CellInfoWcdma -> mapOf(
                        "cellId" to cell.cellIdentity.cid,
                        "lac"    to cell.cellIdentity.lac,
                        "dbm"    to cell.cellSignalStrength.dbm
                    )
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // CellInfoNr (5G) — access via reflection to avoid hard API-level dependency
                        try {
                            val idClass    = Class.forName("android.telephony.CellIdentityNr")
                            val infoClass  = Class.forName("android.telephony.CellInfoNr")
                            if (!infoClass.isInstance(cell)) return@mapNotNull null
                            val identity   = infoClass.getMethod("getCellIdentity").invoke(cell)
                            val signal     = infoClass.getMethod("getCellSignalStrength").invoke(cell)
                            val nci        = idClass.getMethod("getNci").invoke(identity) as? Long ?: return@mapNotNull null
                            val tac        = idClass.getMethod("getTac").invoke(identity) as? Int  ?: return@mapNotNull null
                            val dbm        = signal?.javaClass?.getMethod("getDbm")?.invoke(signal) as? Int ?: return@mapNotNull null
                            mapOf("cellId" to nci.toInt(), "tac" to tac, "dbm" to dbm)
                        } catch (e: Exception) { null }
                    } else null
                }
            }

            if (entries.isEmpty()) return null

            entries.joinToString(",", "[", "]") { entry ->
                entry.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }
            }
        } catch (e: Exception) {
            AppLogger.w("[Metadata] CellInfo unavailable: ${e.message}")
            null
        }
    }
}
