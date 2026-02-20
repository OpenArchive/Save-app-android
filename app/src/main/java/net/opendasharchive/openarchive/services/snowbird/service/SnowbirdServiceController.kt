package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.content.Intent

interface SnowbirdServiceController {
    fun startService()
    fun stopService()
}

class SnowbirdServiceControllerImpl(
    private val context: Context
) : SnowbirdServiceController {

    override fun startService() {
        val intent = Intent(context, SnowbirdService::class.java)
        context.startForegroundService(intent)
    }

    override fun stopService() {
        val intent = Intent(context, SnowbirdService::class.java)
        context.stopService(intent)
    }
}
