package net.opendasharchive.openarchive.extensions

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Patterns
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.graphics.createBitmap

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

fun String.asQRCode(
    size: Int = 512,
    onColor: Int = Color.BLACK,
    offColor: Int = Color.WHITE,
    quietZone: Int = 4
): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.MARGIN, quietZone)
    }
    val bits = QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, size, size, hints)

    // Optimized: Use setPixels (plural) to push the whole array at once
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (bits[x, y]) onColor else offColor
        }
    }

    return createBitmap(size, size).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
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