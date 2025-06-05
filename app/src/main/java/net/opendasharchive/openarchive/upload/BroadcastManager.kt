package net.opendasharchive.openarchive.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object BroadcastManager {

    enum class Action(
        val id: String,
        var collectionId: Long = -1,
        var mediaId: Long = -1,
        var progress: Int = -1,
        var isUploaded: Boolean = false
    ) {
        Change("media_change_intent"),
        Delete("media_delete_intent")
    }

    private const val MEDIA_ID = "media_id"
    private const val COLLECTION_ID = "collection_id"
    private const val MEDIA_PROGRESS = "media_progress"
    private const val MEDIA_UPLOADED = "media_uploaded"

    fun postSuccess(context: Context, collectionId: Long, mediaId: Long) {
        val i = Intent(Action.Change.id)
        i.putExtra(MEDIA_ID, mediaId)
        i.putExtra(COLLECTION_ID, collectionId)
        i.putExtra(MEDIA_UPLOADED, true)
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(i)
    }

    fun postChange(context: Context, collectionId: Long, mediaId: Long) {
        val i = Intent(Action.Change.id)
        i.putExtra(MEDIA_ID, mediaId)
        i.putExtra(COLLECTION_ID, collectionId)
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(i)
    }

    fun postProgress(context: Context, collectionId: Long, mediaId: Long, progress: Int) {
        val i = Intent(Action.Change.id)
        i.putExtra(MEDIA_ID, mediaId)
        i.putExtra(COLLECTION_ID, collectionId)
        i.putExtra(MEDIA_PROGRESS, progress)

        LocalBroadcastManager.getInstance(context).sendBroadcastSync(i)
    }

    fun postDelete(context: Context, mediaId: Long) {
        val i = Intent(Action.Delete.id)
        i.putExtra(MEDIA_ID, mediaId)

        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    fun getAction(intent: Intent): Action? {
        val action = Action.entries.firstOrNull { it.id == intent.action }
        action?.mediaId = intent.getLongExtra(MEDIA_ID, -1)
        action?.collectionId = intent.getLongExtra(COLLECTION_ID, -1)
        action?.progress = intent.getIntExtra(MEDIA_PROGRESS, -1)
        action?.isUploaded = intent.getBooleanExtra(MEDIA_UPLOADED, false)
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
