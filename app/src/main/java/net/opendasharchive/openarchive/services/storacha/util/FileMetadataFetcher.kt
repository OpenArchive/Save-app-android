package net.opendasharchive.openarchive.services.storacha.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.regex.Pattern

data class FileMetadata(
    val fileName: String,
    val fileSize: String,
    val fileType: FileType,
    val directUrl: String,
)

enum class FileType(
    val iconRes: Int,
) {
    IMAGE(R.drawable.ic_image_gallery_line),
    VIDEO(R.drawable.ic_videocam_black_24dp),
    AUDIO(R.drawable.audio_waveform),
    UNKNOWN(R.drawable.ic_unknown_file),
}

class FileMetadataFetcher(
    private val client: OkHttpClient,
) {
    suspend fun fetchFileMetadata(gatewayUrl: String): FileMetadata? =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    Request
                        .Builder()
                        .url(gatewayUrl)
                        .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Timber.e("Failed to fetch metadata: ${response.code}")
                    return@withContext null
                }

                val html = response.body?.string() ?: return@withContext null
                response.close()

                parseFileMetadata(html, gatewayUrl)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching file metadata")
                null
            }
        }

    private fun parseFileMetadata(
        html: String,
        baseUrl: String,
    ): FileMetadata? {
        try {
            // Pattern to match IPFS directory file links
            // Looks for: <a href="/ipfs/hash/filename">filename</a>
            val linkPattern =
                Pattern.compile("<a\\s+href=\"(?:/ipfs/[^/]+/)?([^\"]+)\">([^<]+)</a>")
            val matcher = linkPattern.matcher(html)

            while (matcher.find()) {
                val href = matcher.group(1)?.trim() ?: continue
                val linkText = matcher.group(2)?.trim() ?: continue

                // Skip parent directory links and empty links
                if (href == "../" || linkText == ".." || href.isEmpty() || linkText.isEmpty()) continue

                // Use the link text as filename (this should be the actual filename)
                val fileName = linkText

                // Skip if this looks like a hash rather than a filename
                if (fileName.startsWith("bafy") || fileName.length > 50) continue

                val directUrl = baseUrl.trimEnd('/') + "/" + fileName
                val fileType = determineFileType(fileName)

                // Try to extract file size from the HTML around this link
                val context =
                    try {
                        val start = maxOf(0, matcher.start() - 100)
                        val end = minOf(html.length, matcher.end() + 200)
                        html.substring(start, end)
                    } catch (_: Exception) {
                        html.substring(matcher.end(), minOf(html.length, matcher.end() + 100))
                    }

                val sizePattern =
                    Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*[KMGT]?B)", Pattern.CASE_INSENSITIVE)
                val sizeMatcher = sizePattern.matcher(context)
                val fileSize =
                    if (sizeMatcher.find()) {
                        sizeMatcher.group(1) ?: "Unknown size"
                    } else {
                        "Unknown size"
                    }

                Timber.d("Parsed file: $fileName, size: $fileSize, direct URL: $directUrl")

                return FileMetadata(
                    fileName = fileName,
                    fileSize = fileSize,
                    fileType = fileType,
                    directUrl = directUrl,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing file metadata from HTML")
        }
        return null
    }

    private fun determineFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "avif", "heic", "heif" -> FileType.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v" -> FileType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" -> FileType.AUDIO
            else -> FileType.UNKNOWN
        }
    }
}
