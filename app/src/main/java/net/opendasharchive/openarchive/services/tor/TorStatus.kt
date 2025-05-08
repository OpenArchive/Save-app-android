package net.opendasharchive.openarchive.services.tor

enum class TorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    VERIFIED,
    DISCONNECTING,
    ERROR;
}

data class CheckTorResponse(
    val IsTor: Boolean,
    val IP: String,
)