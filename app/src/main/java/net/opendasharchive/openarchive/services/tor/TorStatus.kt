package net.opendasharchive.openarchive.services.tor

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
    data class Verified(val exitIp: String) : TorStatus()

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
    val error: String? = null
)
