package net.opendasharchive.openarchive.services.tor

enum class TorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    TIMEOUT,
    UNAVAILABLE
}