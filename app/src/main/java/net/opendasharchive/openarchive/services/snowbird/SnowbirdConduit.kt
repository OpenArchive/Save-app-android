package net.opendasharchive.openarchive.services.snowbird

import android.content.Context
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.services.Conduit
import timber.log.Timber

class SnowbirdConduit (evidence: Evidence, context: Context) : Conduit(evidence, context) {
    override suspend fun upload(): Boolean {
        Timber.d("upload")
        return true
    }

    override suspend fun createFolder(url: String) {
        Timber.d("createFolder")
    }
}