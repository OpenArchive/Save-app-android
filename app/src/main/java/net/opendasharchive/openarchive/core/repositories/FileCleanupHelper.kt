package net.opendasharchive.openarchive.core.repositories

import android.content.Context
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.sugar.Media
import java.io.File

/**
 * Helper to handle physical file cleanup for media assets and their metadata.
 */
class FileCleanupHelper(private val context: Context) {

    /**
     * Deletes physical files associated with an Evidence domain object.
     */
    fun deleteMediaFiles(evidence: Evidence) {
        // Delete original media file
        if (evidence.originalFilePath.isNotEmpty()) {
            try {
                val file = evidence.file
                if (isInternalFile(file)) {
                    if (file.exists() && file.delete()) {
                        AppLogger.i("Deleted internal media file: ${file.path}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to delete media file for ${evidence.id}", e)
            }
        }

        // Delete ProofMode metadata
        deleteProofMetadata(evidence.mediaHashString)
    }

    /**
     * Deletes physical files associated with a legacy Media Sugar entity.
     */
    fun deleteMediaFiles(media: Media) {
        // Delete original media file
        if (media.originalFilePath.isNotEmpty()) {
            try {
                val file = media.file
                if (isInternalFile(file)) {
                    if (file.exists() && file.delete()) {
                        AppLogger.i("Deleted internal media file: ${file.path}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to delete legacy media file for ${media.id}", e)
            }
        }

        // Delete ProofMode metadata
        deleteProofMetadata(media.mediaHashString)
    }

    /**
     * Deletes ProofMode metadata directory associated with a media hash.
     */
    private fun deleteProofMetadata(hash: String) {
        if (hash.isEmpty()) return

        // ProofMode metadata cleanup would go here if ProofMode is available
        AppLogger.d("Skipping ProofMode metadata cleanup for hash: $hash (ProofMode not linked)")
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
