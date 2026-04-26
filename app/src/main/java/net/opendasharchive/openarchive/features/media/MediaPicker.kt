package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.net.Uri
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.util.C2paHelper
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.MediaThumbnailGenerator
import net.opendasharchive.openarchive.util.MetadataCollector
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.toLocalDateTime
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MediaPicker {
    // Debug-only artificial delay per media item import for UX/testing.
    private const val DEBUG_IMPORT_DELAY_MS = 1200L

    suspend fun import(
        context: Context,
        archive: Archive,
        submissionId: Long,
        uris: List<Uri>,
        fromCamera: Boolean = false,
    ): ArrayList<Evidence> {
        val result = ArrayList<Evidence>()

        for (uri in uris) {
            try {
                val evidence = import(context, archive, submissionId, uri, fromCamera)
                if (evidence != null) result.add(evidence)
            } catch (e: Exception) {
                AppLogger.e("Error importing media", e)
            }
        }

        return result
    }

    suspend fun import(
        context: Context,
        archive: Archive,
        submissionId: Long,
        uri: Uri,
        fromCamera: Boolean = false,
    ): Evidence? {

        val title = Utility.getUriDisplayName(context, uri)
            ?: uri.lastPathSegment
            ?: uri.path?.substringAfterLast('/')
            ?: ""
        val file  = Utility.getOutputMediaFile(context, title.ifBlank { "media" })

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
        var createDate    = DateUtils.now
        var contentLength = 0L

        if (fileSource?.exists() == true) {
            createDate    = fileSource.lastModified()
            contentLength = fileSource.length()
        } else {
            contentLength = file?.length() ?: 0
        }

        val mimeType = getMimeTypeWithFallback(context, uri, file?.path)

        // --- Collect full device / environment metadata ---
        // Always attempt location; MetadataCollector gates on ACCESS_FINE_LOCATION permission
        val captureMetadata = MetadataCollector.collectMetadata(context)

        // --- Write EXIF to the file BEFORE hashing so the hash covers the final bytes ---
        if (file != null && mimeType.startsWith("image/")) {
            try {
                MetadataCollector.writeExifMetadata(file, captureMetadata)
            } catch (e: Exception) {
                // Non-fatal: log and continue; some image formats (e.g. GIF) don't support EXIF
                AppLogger.w("EXIF write skipped for ${file.name}: ${e.message}")
            }
        }

        // --- Hash the file (after EXIF so the hash reflects the final stored bytes) ---
        val mediaHashString = try {
            file?.let { f ->
                val digest = MessageDigest.getInstance("SHA-256")
                f.inputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        } catch (e: Exception) {
            AppLogger.e("Failed to generate hash for media", e)
            ""
        }

        val thumbnail = try {
            file?.let { MediaThumbnailGenerator.generateThumbnailBytes(it, mimeType) }
        } catch (e: Exception) {
            AppLogger.e("Failed to generate thumbnail for media", e)
            null
        }

        val originalFilePath = Uri.fromFile(file).toString()

        val evidence = Evidence(
            archiveId        = archive.id,
            submissionId     = submissionId,
            title            = title,
            originalFilePath = originalFilePath,
            thumbnail        = thumbnail,
            mimeType         = mimeType,
            contentLength    = contentLength,
            createdAt        = createDate.toLocalDateTime(),
            updatedAt        = createDate.toLocalDateTime(),
            status           = EvidenceStatus.LOCAL,
            mediaHashString  = mediaHashString
        )

        // C2PA: only for in-app camera captures of image/video — not imports, not PDFs
        val isCameraMedia = fromCamera &&
            (mimeType.startsWith("image/") || mimeType.startsWith("video/"))
        if (file != null && mediaHashString.isNotEmpty() && isCameraMedia) {
            C2paHelper.generateManifest(
                context   = context,
                mediaFile = file,
                mediaHash = mediaHashString,
                metadata  = buildProofMetadata(
                    evidence        = evidence,
                    mediaFile       = file,
                    mediaHash       = mediaHashString,
                    captureMetadata = captureMetadata
                )
            )
        }

        return evidence
    }

    /**
     * Build a metadata map whose keys match the ProofMode v1.0.25 field names.
     * This is written into the C2PA sidecar JSON so proofs are interoperable with
     * existing ProofMode verification tools.
     */
    private fun buildProofMetadata(
        evidence       : Evidence,
        mediaFile      : File,
        mediaHash      : String,
        captureMetadata: MetadataCollector.CaptureMetadata
    ): Map<String, String> {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }

        return buildMap {
            // --- File ---
            put("File Hash SHA256", mediaHash)
            put("File Path",        evidence.originalFilePath)
            put("File Created",     isoFmt.format(Date(captureMetadata.captureTime)))
            put("File Modified",    isoFmt.format(Date(mediaFile.lastModified())))

            // --- Proof ---
            put("Proof Generated",  isoFmt.format(Date(captureMetadata.captureTime)))
            put("Notes", "${captureMetadata.appName} ${captureMetadata.appVersion}")

            // --- Device ---
            put("Manufacturer", captureMetadata.deviceMake)
            // ProofMode "Hardware" is make + model, e.g. "samsung SM-S9010"
            put("Hardware", "${captureMetadata.deviceMake} ${captureMetadata.deviceModel}")

            // --- Locale / language ---
            put("Locale",   captureMetadata.locale)
            put("Language", captureMetadata.language)

            // --- Screen ---
            captureMetadata.screenSizeInches?.let { put("ScreenSize", it.toString()) }

            // --- Location ---
            captureMetadata.latitude?.let         { put("Location.Latitude",  it.toString()) }
            captureMetadata.longitude?.let        { put("Location.Longitude", it.toString()) }
            captureMetadata.locationAltitude?.let { put("Location.Altitude",  it.toString()) }
            captureMetadata.locationAccuracy?.let { put("Location.Accuracy",  it.toString()) }
            captureMetadata.locationBearing?.let  { put("Location.Bearing",   it.toString()) }
            captureMetadata.locationSpeed?.let    { put("Location.Speed",     it.toString()) }
            captureMetadata.locationTime?.let     { put("Location.Time",      it.toString()) }
            captureMetadata.locationProvider?.let { put("Location.Provider",  it) }

            // --- Network ---
            captureMetadata.networkType?.let { put("NetworkType", it) }
            // DataType mirrors NetworkType label (ProofMode uses both)
            captureMetadata.networkType?.let { put("DataType", it) }
            captureMetadata.ipv4?.let        { put("IPv4", it) }
            captureMetadata.ipv6?.let        { put("IPv6", it) }

            // --- Cell ---
            captureMetadata.cellInfo?.let { put("CellInfo", it) }

            // --- Extra fields useful for Save ---
            put("mimeType", evidence.mimeType)
            put("title",    evidence.title)
        }
    }

    /**
     * Enhanced mime type detection that falls back to file extension detection
     * for file URIs where ContentResolver might not have mime type info.
     */
    private fun getMimeTypeWithFallback(context: Context, uri: Uri, filePath: String?): String {
        val standardMimeType = Utility.getMimeType(context, uri)
        if (!standardMimeType.isNullOrEmpty()) return standardMimeType

        val extension = when {
            filePath != null -> File(filePath).extension
            uri.path != null -> File(uri.path!!).extension
            else             -> null
        }

        return when (extension?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "gif"         -> "image/gif"
            "webp"        -> "image/webp"
            "mp4"         -> "video/mp4"
            "mov"         -> "video/quicktime"
            "avi"         -> "video/x-msvideo"
            "mkv"         -> "video/x-matroska"
            "webm"        -> "video/webm"
            "mp3"         -> "audio/mpeg"
            "wav"         -> "audio/wav"
            "ogg"         -> "audio/ogg"
            "m4a"         -> "audio/mp4"
            else -> {
                AppLogger.w("Unknown file extension '$extension' for URI: $uri")
                "application/octet-stream"
            }
        }
    }
}
