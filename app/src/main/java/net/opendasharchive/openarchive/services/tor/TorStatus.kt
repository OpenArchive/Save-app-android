package net.opendasharchive.openarchive.services.tor

/**
 * Connection information for a verified Tor connection.
 *
 * @property exitIp The IP address of the Tor exit node
 * @property exitCountry The country of the exit node (from IP geolocation), null if lookup failed
 */
data class TorConnectionInfo(
    val exitIp: String,
    val exitCountry: String? = null
)

/**
 * Represents the current status of the embedded Tor service.
 */
sealed class TorStatus {
    /** Tor service is idle and not running */
    object Idle : TorStatus()

    /** Tor service is starting up */
    object Starting : TorStatus()

    /** Tor service is connected and ready (not yet verified) */
    object On : TorStatus()

    /** Tor service is connected AND verified to be routing through Tor */
    data class Verified(val info: TorConnectionInfo) : TorStatus()

    /** Tor service is stopped/disabled */
    object Off : TorStatus()

    /** Tor service encountered an error */
    data class Error(val message: String) : TorStatus()
}

/**
 * Result of Tor connection verification.
 */
data class TorVerificationResult(
    val isUsingTor: Boolean,
    val exitIp: String? = null,
    val exitCountry: String? = null,
    val error: String? = null
)
