package net.opendasharchive.openarchive.services.tor

/**
 * Constants for Tor service configuration.
 *
 * SECURITY NOTE: The SOCKS port is dynamically allocated for security reasons.
 * Never hardcode port 9050 as it's predictable and could be exploited by malicious apps.
 * Always use TorServiceManager.socksPort.value to get the actual port.
 */
object TorConstants {
    /** SOCKS5 proxy address (localhost) */
    const val SOCKS5_PROXY_ADDRESS = "127.0.0.1"

    /** Tor notification ID */
    const val TOR_NOTIFICATION_ID = 2602

    /** Tor notification channel ID */
    const val TOR_NOTIFICATION_CHANNEL_ID = "tor_service_channel"

    /** Default torrc configuration for security (random port allocation) */
    const val DEFAULT_TORRC_CONFIG = """
SocksPort auto
HTTPTunnelPort auto
"""
}
