package net.opendasharchive.openarchive.core.repositories

import android.app.Application
import android.net.Uri
import net.opendasharchive.openarchive.core.domain.Evidence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FileCleanupHelperTest {

    @Test
    fun `deleteUploadedMediaFiles removes internal file and keeps db-backed thumbnail untouched`() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.filesDir, "cleanup-uploaded-file.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val evidence = Evidence(
            originalFilePath = Uri.fromFile(file).toString(),
            thumbnail = byteArrayOf(9, 8, 7),
            mediaHashString = ""
        )

        FileCleanupHelper(context).deleteUploadedMediaFiles(evidence)

        assertFalse(file.exists())
        assertTrue(evidence.thumbnail!!.isNotEmpty())
    }
}
