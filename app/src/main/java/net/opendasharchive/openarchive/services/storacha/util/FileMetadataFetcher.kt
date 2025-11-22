package net.opendasharchive.openarchive.services.storacha.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import okhttp3.OkHttpClient
import okhttp3.Request
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
    IMAGE(R.drawable.ic_image),
    VIDEO(R.drawable.ic_video),
    AUDIO(R.drawable.ic_music),
    PDF(R.drawable.ic_pdf),
    DOCUMENT(R.drawable.ic_doc),
    ZIP(R.drawable.ic_zip),
    UNKNOWN(R.drawable.ic_unknown),
}

class FileMetadataFetcher(
    private val client: OkHttpClient,
) {
    // Pre-compile patterns for better performance
    private val linkPattern = Pattern.compile("<a\\s+href=\"(?:/ipfs/[^/]+/)?([^\"]+)\">([^<]+)</a>")
    private val sizePattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*[KMGT]?B)", Pattern.CASE_INSENSITIVE)
    private val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toSet()

    suspend fun fetchFileMetadata(gatewayUrl: String): FileMetadata? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(gatewayUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()?.let { parseFileMetadata(it, gatewayUrl) }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFileMetadata(html: String, baseUrl: String): FileMetadata? {
        val matcher = linkPattern.matcher(html)

        while (matcher.find()) {
            val href = matcher.group(1)?.trim() ?: continue
            val fileName = matcher.group(2)?.trim() ?: continue

            if (href == "../" || fileName == ".." || href.isEmpty() || fileName.isEmpty()) continue
            if (isIpfsHash(fileName)) continue

            val directUrl = "${baseUrl.trimEnd('/')}/$fileName"
            val fileType = determineFileType(fileName)

            // Extract file size from context around the link
            val start = maxOf(0, matcher.start() - 100)
            val end = minOf(html.length, matcher.end() + 200)
            val context = html.substring(start, end)
            val sizeMatcher = sizePattern.matcher(context)
            val fileSize = if (sizeMatcher.find()) sizeMatcher.group(1) ?: "Unknown size" else "Unknown size"

            return FileMetadata(fileName, fileSize, fileType, directUrl)
        }
        return null
    }

    private fun isIpfsHash(text: String): Boolean = when {
        text.startsWith("Qm") && text.length == 46 && text.all { it in base58Chars } -> true
        text.startsWith("ba") && text.length in 52..64 && '.' !in text -> true
        text.length > 100 && '.' !in text -> true
        else -> false
    }

    private fun determineFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "avif", "heic", "heif" -> FileType.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v" -> FileType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" -> FileType.AUDIO
            "pdf" -> FileType.PDF
            "zip" -> FileType.ZIP
            "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv" -> FileType.DOCUMENT
            else -> FileType.UNKNOWN
        }
    }
}
