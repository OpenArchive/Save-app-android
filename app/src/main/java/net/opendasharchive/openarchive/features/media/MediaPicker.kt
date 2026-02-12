package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.net.Uri
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.toLocalDateTime
import org.witness.proofmode.ProofMode
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

object MediaPicker {
    // Debug-only artificial delay per media item import for UX/testing.
    private const val DEBUG_IMPORT_DELAY_MS = 1200L

    fun import(
        context: Context,
        archive: Archive,
        submissionId: Long,
        uris: List<Uri>,
        generateProof: Boolean
    ): ArrayList<Evidence> {
        val result = ArrayList<Evidence>()

        for (uri in uris) {
            try {
                //Simply pass the generate proof boolean for single file import which is looped here
                val evidence = import(context, archive, submissionId, uri, generateProof)
                if (evidence != null) result.add(evidence)
            } catch (e: Exception) {
                AppLogger.e("Error importing media", e)
            }
        }

        return result
    }

    fun import(
        context: Context,
        archive: Archive,
        submissionId: Long,
        uri: Uri,
        generateProof: Boolean
    ): Evidence? {

        val title = Utility.getUriDisplayName(context, uri) ?: ""
        // TODO: This is a temporary persistent storage solution. 
        // Review this when implementing the new Evidence architecture.
        val file = Utility.getOutputMediaFile(context, title)

        // Use try-with-resources pattern for proper resource management
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                if (!Utility.writeStreamToFile(inputStream, file)) {
                    AppLogger.e("Failed to write stream to file for URI: $uri")
                    return null
                }
            } ?: run {
                AppLogger.e("Failed to open input stream for URI: $uri")
                return null
            }
        } catch (e: FileNotFoundException) {
            AppLogger.e("File not found for URI: $uri", e)
            return null
        } catch (e: SecurityException) {
            AppLogger.e("Permission denied for URI: $uri", e)
            return null
        } catch (e: java.io.IOException) {
            AppLogger.e("IO error reading URI: $uri", e)
            return null
        }

        val fileSource = uri.path?.let { File(it) }
        var createDate = DateUtils.now
        var contentLength = 0L

        if (fileSource?.exists() == true) {
            createDate = fileSource.lastModified()
            contentLength = fileSource.length()
        } else {
            contentLength = file?.length() ?: 0
        }

        val originalFilePath = Uri.fromFile(file).toString()
        // Enhanced mime type detection for file URIs
        val mimeType = getMimeTypeWithFallback(context, uri, file?.path)

        // Generate hash regardless of proof mode setting for consistency
        val mediaHashString = try {
            file?.let {
                HashUtils.getSHA256FromFileContent(it.inputStream())
            } ?: ""
        } catch (e: Exception) {
            AppLogger.e("Failed to generate hash for media", e)
            ""
        }

        // Create domain object
        val evidence = Evidence(
            archiveId = archive.id,
            submissionId = submissionId,
            title = title,
            originalFilePath = originalFilePath,
            mimeType = mimeType,
            contentLength = contentLength,
            createdAt = createDate.toLocalDateTime(),
            updatedAt = createDate.toLocalDateTime(),
            status = net.opendasharchive.openarchive.core.domain.EvidenceStatus.LOCAL,
            mediaHashString = mediaHashString
        )

        // Generate ProofMode data if enabled
        if (generateProof && Prefs.useProofMode) {

            try {
                //If Proof mode is on we need this to be on always
                // Ensure location and network tracking are enabled for camera captures
                // Only enabled for camera captures (generateProof = true)
                Prefs.proofModeLocation = true
                Prefs.proofModeNetwork = true

                AppLogger.d("Generating ProofMode data for URI: $uri, Hash: ${evidence.mediaHashString}")

                // Generate proof using the ProofMode library
                ProofMode.generateProof(context, uri, evidence.mediaHashString)

                AppLogger.i("ProofMode generation completed for media: ${evidence.title}")
            } catch (e: Exception) {
                AppLogger.e("Failed to generate ProofMode data", e)
                Timber.w("ProofMode generation failed: ${e.message}")
            }
        } else {
            if (generateProof) {
                AppLogger.w("ProofMode generation requested but useProofMode is disabled")
            }
            Timber.w("Skipping proof generation - generateProof: $generateProof, useProofMode: ${Prefs.useProofMode}")
        }
        return evidence
    }

    /**
     * Enhanced mime type detection that falls back to file extension detection
     * for file URIs where ContentResolver might not have mime type info.
     */
    private fun getMimeTypeWithFallback(context: Context, uri: Uri, filePath: String?): String {
        // First try the standard way
        val standardMimeType = Utility.getMimeType(context, uri)
        if (!standardMimeType.isNullOrEmpty()) {
            return standardMimeType
        }

        // Fallback to file extension detection
        val extension = when {
            filePath != null -> File(filePath).extension
            uri.path != null -> File(uri.path!!).extension
            else -> null
        }

        return when (extension?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            else -> {
                AppLogger.w("Unknown file extension '$extension' for URI: $uri")
                "application/octet-stream" // Generic binary type
            }
        }
    }
}
