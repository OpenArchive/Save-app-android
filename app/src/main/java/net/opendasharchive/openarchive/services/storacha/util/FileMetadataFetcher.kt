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
            // Use HEAD request first to check content type without downloading the full content
            val headRequest = Request.Builder().url(gatewayUrl).head().build()
            val headResponse = client.newCall(headRequest).execute()

            if (!headResponse.isSuccessful) {
                headResponse.close()
                return@withContext null
            }

            val contentType = headResponse.header("Content-Type") ?: ""
            val contentLength = headResponse.header("Content-Length")?.toLongOrNull()
            val contentDisposition = headResponse.header("Content-Disposition")
            headResponse.close()

            // If content type indicates direct file content (not HTML directory listing),
            // create metadata directly from headers
            if (!contentType.contains("text/html", ignoreCase = true)) {
                return@withContext createMetadataFromHeaders(
                    contentType = contentType,
                    contentLength = contentLength,
                    contentDisposition = contentDisposition,
                    directUrl = gatewayUrl,
                )
            }

            // It's HTML - fetch and parse directory listing
            val request = Request.Builder().url(gatewayUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()?.let { parseFileMetadata(it, gatewayUrl) }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates FileMetadata from HTTP headers when the gateway returns
     * file content directly (not wrapped in a directory).
     */
    private fun createMetadataFromHeaders(
        contentType: String,
        contentLength: Long?,
        contentDisposition: String?,
        directUrl: String,
    ): FileMetadata {
        val fileType = determineFileTypeFromContentType(contentType)

        // Try to extract filename from Content-Disposition header first
        // Format: attachment; filename="example.jpg" or inline; filename="example.jpg"
        val fileName = extractFilenameFromContentDisposition(contentDisposition)
            ?: run {
                // Fallback: use truncated CID as display name
                val cid = extractCidFromUrl(directUrl)
                if (cid.length > 12) "${cid.take(8)}...${cid.takeLast(4)}" else cid
            }

        // Format file size
        val fileSize = contentLength?.let { formatFileSize(it) } ?: "Unknown size"

        return FileMetadata(
            fileName = fileName,
            fileSize = fileSize,
            fileType = fileType,
            directUrl = directUrl,
        )
    }

    /**
     * Extracts filename from Content-Disposition header.
     * Handles formats like:
     * - attachment; filename="example.jpg"
     * - inline; filename="example.jpg"
     * - attachment; filename*=UTF-8''example.jpg
     */
    private fun extractFilenameFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null

        // Try filename="..." format first
        val quotedPattern = Pattern.compile("filename\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val quotedMatcher = quotedPattern.matcher(contentDisposition)
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1)
        }

        // Try filename=... format (without quotes)
        val unquotedPattern = Pattern.compile("filename\\s*=\\s*([^;\\s]+)", Pattern.CASE_INSENSITIVE)
        val unquotedMatcher = unquotedPattern.matcher(contentDisposition)
        if (unquotedMatcher.find()) {
            return unquotedMatcher.group(1)
        }

        // Try filename*=UTF-8''... format (RFC 5987)
        val encodedPattern = Pattern.compile("filename\\*\\s*=\\s*[^']*'[^']*'([^;\\s]+)", Pattern.CASE_INSENSITIVE)
        val encodedMatcher = encodedPattern.matcher(contentDisposition)
        if (encodedMatcher.find()) {
            return try {
                java.net.URLDecoder.decode(encodedMatcher.group(1), "UTF-8")
            } catch (_: Exception) {
                encodedMatcher.group(1)
            }
        }

        return null
    }

    /**
     * Extracts CID from various gateway URL formats:
     * - Path-based: https://gateway.com/ipfs/bafyxxx
     * - Subdomain-based: https://bafyxxx.ipfs.dweb.link or https://bafyxxx.dweb.link
     */
    private fun extractCidFromUrl(url: String): String {
        // Try path-based format first: /ipfs/CID
        if (url.contains("/ipfs/")) {
            val cid = url.substringAfter("/ipfs/").substringBefore("/").substringBefore("?")
            if (cid.startsWith("bafy") || cid.startsWith("Qm") || cid.startsWith("baf")) {
                return cid
            }
        }

        // Try subdomain-based format: CID.ipfs.gateway.com or CID.gateway.com
        try {
            val host = url.substringAfter("://").substringBefore("/").substringBefore("?")
            val subdomain = host.substringBefore(".")
            if (subdomain.startsWith("bafy") || subdomain.startsWith("Qm") || subdomain.startsWith("baf")) {
                return subdomain
            }
        } catch (_: Exception) {
            // Fall through to default
        }

        // Fallback: return a truncated version of the URL
        return "file"
    }

    /**
     * Determines FileType from Content-Type header.
     */
    private fun determineFileTypeFromContentType(contentType: String): FileType {
        val mimeType = contentType.substringBefore(";").trim().lowercase()
        return when {
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("video/") -> FileType.VIDEO
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType == "application/pdf" -> FileType.PDF
            mimeType == "application/zip" ||
                mimeType == "application/x-zip-compressed" ||
                mimeType == "application/x-tar" ||
                mimeType == "application/gzip" -> FileType.ZIP
            mimeType.startsWith("text/") ||
                mimeType == "application/msword" ||
                mimeType.contains("document") ||
                mimeType.contains("spreadsheet") ||
                mimeType.contains("presentation") -> FileType.DOCUMENT
            else -> FileType.UNKNOWN
        }
    }

    /**
     * Formats file size in human-readable format.
     */
    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
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
