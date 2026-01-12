package net.opendasharchive.openarchive.util

import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import kotlin.coroutines.resume

/**
 * GMS implementation of LocationProvider using FusedLocationProviderClient.
 * Used in Google Play builds.
 */
class GmsLocationProvider(private val context: Context) : LocationProvider {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(timeoutMs: Long): Location? {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val cancellationTokenSource = CancellationTokenSource()

                    // Create location request with high accuracy
                    val locationRequest = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdateAgeMillis(0)  // Don't accept cached locations
                        .build()

                    fusedLocationClient.getCurrentLocation(
                        locationRequest,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            if (location != null) {
                                AppLogger.d("[GmsLocation] Got location: ${location.latitude}, ${location.longitude}")
                            } else {
                                AppLogger.w("[GmsLocation] Location is null")
                            }
                            continuation.resume(location)
                        }
                    }.addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            AppLogger.w("[GmsLocation] Failed to get location: ${exception.message}")
                            continuation.resume(null)
                        }
                    }

                    // Handle cancellation
                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }

                    // Set timeout
                    GlobalScope.launch {
                        delay(timeoutMs)
                        if (continuation.isActive) {
                            AppLogger.w("[GmsLocation] Location request timed out after ${timeoutMs}ms")
                            cancellationTokenSource.cancel()
                            continuation.resume(null)
                        }
                    }
                } catch (e: SecurityException) {
                    AppLogger.e("[GmsLocation] No location permission", e)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    AppLogger.e("[GmsLocation] Unexpected error", e)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
