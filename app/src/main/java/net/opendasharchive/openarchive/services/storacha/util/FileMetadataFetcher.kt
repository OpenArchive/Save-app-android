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
    VIDEO(R.drawable.ic_video_document),
    AUDIO(R.drawable.audio_waveform),
    PDF(R.drawable.ic_pdf_document),
    DOCUMENT(R.drawable.ic_unknown_file),
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

                // Skip if this looks like an IPFS hash rather than a filename
                if (isIpfsHash(fileName)) continue

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

    private fun isIpfsHash(text: String): Boolean {
        // IPFS hash patterns:
        // - CIDv0: Qm... (46 chars, base58)
        // - CIDv1: bafy... bafz... baga... etc. (variable length, typically 52-64 chars)
        // - Other CID prefixes: baae, baai, baan, etc.

        return when {
            // CIDv0 pattern: starts with Qm, exactly 46 characters, base58
            text.startsWith("Qm") && text.length == 46 && text.all { it.isBase58() } -> true

            // CIDv1 pattern: starts with 'ba', typically 52-64 characters, no file extension
            text.startsWith("ba") && text.length in 52..64 && !text.contains('.') -> true

            // Additional safety: if it's very long and has no extension, likely a hash
            text.length > 100 && !text.contains('.') -> true

            else -> false
        }
    }

    private fun Char.isBase58(): Boolean {
        return this in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }

    private fun determineFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "avif", "heic", "heif" -> FileType.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v" -> FileType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" -> FileType.AUDIO
            "pdf" -> FileType.PDF
            "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv" -> FileType.DOCUMENT
            else -> FileType.UNKNOWN
        }
    }
}
