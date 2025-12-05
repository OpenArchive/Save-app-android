package net.opendasharchive.openarchive.extensions

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Patterns
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Generates a QR code bitmap from a given string.
 *
 * @param size The width and height of the resulting bitmap.
 * @param quietZone The size of the quiet zone around the QR code (optional, default is 4).
 * @return A Bitmap containing the generated QR code.
 */
fun String.asQRCode(size: Int = 512, quietZone: Int = 4): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.MARGIN, quietZone)
    }

    val bits = QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, size, size, hints)

    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    }
}

fun String.createInputStream(): InputStream? {
    return try {
        File(this).inputStream()
    } catch (e: Exception) {
        Timber.e(e, "Failed to create InputStream from path: $this")
        null
    }
}

fun String.getQueryParameter(paramName: String): String? {
    val queryStart = this.indexOf('?')
    if (queryStart == -1) return null

    val queryString = this.substring(queryStart + 1)

    return queryString.split('&')
        .map { it.split('=', limit = 2) }
        .find { it.size == 2 && URLDecoder.decode(it[0], StandardCharsets.UTF_8.toString()) == paramName }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
}

fun String.isValidUrl() = Patterns.WEB_URL.matcher(this).matches()

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

fun String.uriToPath(): String {
    val uri = URI(this)
    return uri.path
}