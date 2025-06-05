package net.opendasharchive.openarchive.services.snowbird

sealed class SnowbirdServiceStatus {
    data object BackendInitializing : SnowbirdServiceStatus()
    data object BackendRunning : SnowbirdServiceStatus()
    data object WebServerInitializing : SnowbirdServiceStatus()
    data object WebServerRunning : SnowbirdServiceStatus()
    data object Processing : SnowbirdServiceStatus()
    data object Idle : SnowbirdServiceStatus()
    data class Error(val message: String) : SnowbirdServiceStatus()

    val code: Int
        get() = when (this) {
            is BackendInitializing -> 0
            is BackendRunning -> 1
            is WebServerInitializing -> 2
            is WebServerRunning -> 3
            is Processing -> 4
            is Idle -> 5
            is Error -> 6
        }

    companion object {
        fun fromCode(code: Int, errorMessage: String? = null): SnowbirdServiceStatus =
            when (code) {
                0 -> BackendInitializing
                1 -> BackendRunning
                2 -> WebServerInitializing
                3 -> WebServerRunning
                4 -> Processing
                5 -> Idle
                6 -> Error(errorMessage ?: "Unknown error")
                else -> throw IllegalArgumentException("Invalid status code: $code")
            }
    }
}