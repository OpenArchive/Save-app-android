package net.opendasharchive.openarchive.services.snowbird

import android.content.Context
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import timber.log.Timber

class SnowbirdConduit (media: Media, context: Context) : Conduit(media, context) {
    override suspend fun upload(): Boolean {
        Timber.d("upload")
        return true
    }

    override suspend fun createFolder(url: String) {
        Timber.d("createFolder")
    }
}