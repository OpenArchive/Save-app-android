package net.opendasharchive.openarchive.services.snowbird

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SnowbirdBridge {
    private val _status = MutableStateFlow<SnowbirdServiceStatus>(SnowbirdServiceStatus.BackendInitializing)
    val status = _status.asStateFlow()

    fun initialize() {
        initializeRustService()
    }

    init {
        System.loadLibrary("save")
    }

    companion object {
        @Volatile
        private var instance: SnowbirdBridge? = null

        fun getInstance(): SnowbirdBridge {
            return instance ?: synchronized(this) {
                instance ?: SnowbirdBridge().also { instance = it }
            }
        }

        @JvmStatic
        fun updateStatusFromRust(code: Int, message: String) {
            instance?._status?.value = SnowbirdServiceStatus.fromCode(code)
        }
    }

    private external fun initializeRustService()
    external fun startServer(context: Context, baseDirectory: String, socketPath: String): String
    external fun stopServer()
}