package net.opendasharchive.openarchive.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger

/**
 * FOSS implementation of LocationProvider using LocationManager.
 * Used in F-Droid builds.
 */
class FossLocationProvider(private val context: Context) : LocationProvider {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun getCurrentLocation(timeoutMs: Long): Location? {
        return withContext(Dispatchers.IO) {
            try {
                // Try GPS first, then Network provider
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER
                ).filter { locationManager.isProviderEnabled(it) }

                if (providers.isEmpty()) {
                    AppLogger.w("[FossLocation] No location providers available")
                    return@withContext null
                }

                // Try to get last known location first (fast)
                val lastKnown = providers.firstNotNullOfOrNull { provider ->
                    try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (e: SecurityException) {
                        AppLogger.e("[FossLocation] No permission for provider: $provider", e)
                        null
                    }
                }

                // If last known location is recent (< 2 minutes), use it
                if (lastKnown != null &&
                    System.currentTimeMillis() - lastKnown.time < 120_000) {
                    AppLogger.d("[FossLocation] Using last known location from ${lastKnown.provider}: ${lastKnown.latitude}, ${lastKnown.longitude} (age: ${(System.currentTimeMillis() - lastKnown.time) / 1000}s)")
                    return@withContext lastKnown
                }

                // Otherwise return last known (stale) or null
                if (lastKnown != null) {
                    AppLogger.d("[FossLocation] Using stale last known location from ${lastKnown.provider}: ${lastKnown.latitude}, ${lastKnown.longitude} (age: ${(System.currentTimeMillis() - lastKnown.time) / 1000}s)")
                    return@withContext lastKnown
                }

                AppLogger.w("[FossLocation] No location available from any provider")
                null
            } catch (e: SecurityException) {
                AppLogger.e("[FossLocation] No location permission", e)
                null
            } catch (e: Exception) {
                AppLogger.e("[FossLocation] Unexpected error getting location", e)
                null
            }
        }
    }
}
