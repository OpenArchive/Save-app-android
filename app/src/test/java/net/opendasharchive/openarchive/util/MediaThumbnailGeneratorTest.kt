package net.opendasharchive.openarchive.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MediaThumbnailGeneratorTest {

    @Test
    fun `generateThumbnailBytes creates a small jpeg thumbnail for image files`() {
        val source = File.createTempFile("thumbnail-source", ".jpg")
        source.outputStream().use { output ->
            Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.RED)
                compress(Bitmap.CompressFormat.JPEG, 95, output)
                recycle()
            }
        }

        val thumbnailBytes = MediaThumbnailGenerator.generateThumbnailBytes(
            file = source,
            mimeType = "image/jpeg"
        )

        assertNotNull(thumbnailBytes)
        assertTrue(thumbnailBytes!!.size in 1 until 20_000)

        val bitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
        assertNotNull(bitmap)
        assertTrue(bitmap!!.width <= 128)
        assertTrue(bitmap.height <= 128)

        bitmap.recycle()
        source.delete()
    }
}
