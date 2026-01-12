package net.opendasharchive.openarchive.util

import android.location.Location

/**
 * Interface for location provider abstraction.
 * Allows different implementations for GMS and FOSS builds.
 */
interface LocationProvider {
    /**
     * Get the current location asynchronously.
     *
     * @param timeoutMs Maximum time to wait for location in milliseconds (default: 5000ms)
     * @return Location if available, null otherwise
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 5000): Location?
}
