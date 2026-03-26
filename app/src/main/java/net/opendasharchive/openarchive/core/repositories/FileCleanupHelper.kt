package net.opendasharchive.openarchive.core.repositories

import android.content.Context
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.sugar.Media
import net.opendasharchive.openarchive.util.C2paHelper
import java.io.File

/**
 * Helper to handle physical file cleanup for media assets and their metadata.
 */
class FileCleanupHelper(private val context: Context) {

    /**
     * Deletes local upload artifacts after a successful upload while keeping the DB record.
     */
    fun deleteUploadedMediaFiles(evidence: Evidence) {
        deleteInternalMediaFile(evidence)
        deleteC2paSidecar(evidence.mediaHashString)
    }

    /**
     * Deletes physical files associated with an Evidence domain object.
     */
    fun deleteMediaFiles(evidence: Evidence) {
        deleteUploadedMediaFiles(evidence)
    }

    /**
     * Deletes physical files associated with a legacy Media Sugar entity.
     */
    fun deleteMediaFiles(media: Media) {
        if (media.originalFilePath.isNotEmpty()) {
            try {
                val file = media.file
                if (isInternalFile(file) && file.exists() && file.delete()) {
                    AppLogger.i("Deleted internal media file: ${file.path}")
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to delete legacy media file for ${media.id}", e)
            }
        }

        deleteC2paSidecar(media.mediaHashString)
    }

    private fun deleteInternalMediaFile(evidence: Evidence) {
        if (evidence.originalFilePath.isNotEmpty()) {
            try {
                val file = evidence.file
                if (isInternalFile(file) && file.exists() && file.delete()) {
                    AppLogger.i("Deleted internal media file: ${file.path}")
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to delete media file for ${evidence.id}", e)
            }
        }
    }

    private fun deleteC2paSidecar(mediaHashString: String) {
        if (mediaHashString.isNotEmpty()) {
            C2paHelper.removeC2paFiles(context, mediaHashString)
        }
    }

    /**
     * Checks if a file resides within the application's internal files directory.
     * We only want to delete files we imported to our internal storage.
     */
    private fun isInternalFile(file: File): Boolean {
        val internalDir = context.filesDir.canonicalPath
        return file.canonicalPath.startsWith(internalDir)
    }
}
