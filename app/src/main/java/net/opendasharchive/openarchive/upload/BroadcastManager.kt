package net.opendasharchive.openarchive.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object BroadcastManager {

    enum class Action(val id: String, var mediaId: Long = -1) {
        Change("media_change_intent"),
        Delete("media_delete_intent")
    }

    private const val MEDIA_ID = "media_id"

    fun postChange(context: Context, mediaId: Long) {
        val i = Intent(Action.Change.id)
        i.putExtra(MEDIA_ID, mediaId)

        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    fun postDelete(context: Context, mediaId: Long) {
        val i = Intent(Action.Delete.id)
        i.putExtra(MEDIA_ID, mediaId)

        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    fun getAction(intent: Intent): Action? {
        val action = Action.values().firstOrNull { it.id == intent.action }
        action?.mediaId = intent.getLongExtra(MEDIA_ID, -1)

        return action
    }

    fun register(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, IntentFilter(Action.Change.id))

        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, IntentFilter(Action.Delete.id))
    }

    fun unregister(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }
}