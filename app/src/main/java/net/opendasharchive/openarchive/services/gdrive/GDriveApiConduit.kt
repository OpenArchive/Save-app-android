package net.opendasharchive.openarchive.services.gdrive

import android.content.Context
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.InputStreamContent
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.folders.BrowseFoldersViewModel
import net.opendasharchive.openarchive.services.Conduit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.Date

/**
 * This class contains all communication with / integration of Google Drive
 *
 * The only actually working documentation I could find about Googles Android GDrive API is this:
 * https://stackoverflow.com/questions/56949872/
 * There's also this official documentation by Google for accessing GDrive, however it was pretty
 * useless to me, since it doesn't explain what's going on at all. (I also couldn't get it to run
 * in a reasonable amount of time):
 * https://github.com/googleworkspace/android-samples/tree/master/drive/deprecation
 * The official documentation doesn't mention Android and the Java Sample is only useful for
 * integrating GDrive into backends. However it's still helpful for figuring building queries:
 * https://developers.google.com/drive/api/guides/about-sdk
 * Another important resource is this official guide on authenticating an Android app with Google:
 * https://developers.google.com/identity/sign-in/android/start-integrating
 */
class GDriveApiConduit(media: Media, context: Context) : Conduit(media, context), KoinComponent {

    private val drive: GDriveRepository by inject()

    companion object {

        const val NAME = "Google Drive"
        val SCOPE_NAMES =
            arrayOf(Scopes.EMAIL, Scopes.DRIVE_FILE)
        val SCOPES = SCOPE_NAMES.map { Scope(it) }.toTypedArray()
    }

    private suspend fun getOrCreateFolder(folderName: String, parent: GDriveFile? = null): GDriveFile {
        val folder = drive.folders(pageSize = 1).getOrNull()

        if (folder == null || folder.files.isEmpty()) {
            return drive.newFolder(folderName, parent?.id).getOrThrow()
        }
        return folder.files.first()
    }

    suspend fun createFolders(destinationPath: List<String>): GDriveFileList = try {
        var parentFolder: GDriveFile? = null
        val result = mutableListOf<GDriveFile>()
        for (pathElement in destinationPath) {
            parentFolder = getOrCreateFolder(pathElement, parentFolder)
            result.add(parentFolder)
        }
        GDriveFileList(result)
    } catch (e: Exception) {
        throw e
    }

    suspend fun listFoldersInRoot(): List<BrowseFoldersViewModel.Folder> {
        val result = ArrayList<BrowseFoldersViewModel.Folder>()
        try {
            var pageToken: String? = null
            do {
                val folders = drive.folders("root", 1000, pageToken).getOrThrow()
                folders.files.forEach {
                    result.add(BrowseFoldersViewModel.Folder(it.name, Date(it.modifiedTime)))
                }
                pageToken = folders.nextPageToken
            } while (pageToken != null)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
        return result
    }

    override suspend fun upload(): Boolean {
        val destinationPath = getPath() ?: return false
        val destinationFileName = getUploadFileName(mMedia)
        sanitize()

        try {
            val folder = createFolders(destinationPath).files.last()
            uploadMetadata(folder, destinationFileName)
            if (mCancelled) throw Exception("Cancelled")
            uploadFile(mMedia.file, folder, destinationFileName)
        } catch (e: Exception) {
            jobFailed(e)
            return false
        }

        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        throw NotImplementedError("the createFolder calls defined in Conduit don't map to GDrive API. use GDriveConduit.createFolder instead")
    }

    private suspend fun uploadMetadata(parent: GDriveFile, fileName: String) {
        val metadataFileName = "$fileName.meta.json"

        if (mCancelled) throw Exception("Cancelled")

        uploadFile(getMetadata().byteInputStream(), parent, metadataFileName)

        for (file in getProof()) {
            if (mCancelled) throw Exception("Cancelled")

            uploadFile(file, parent, file.name)
        }
    }

    private suspend fun uploadFile(
        sourceFile: File,
        parentFolder: GDriveFile,
        targetFileName: String,
    ) = uploadFile(sourceFile.inputStream(), parentFolder, targetFileName)

    private suspend fun uploadFile(
        inputStream: InputStream,
        parentFolder: GDriveFile,
        targetFileName: String,
    ) =
        try {
            val fMeta = GDriveFile(name = targetFileName, parents = listOf(parentFolder.id!!))
            drive.upload(fMeta, InputStreamContent("application/octet-stream", inputStream)) { bytesWritten, percent ->
                jobProgress(bytesWritten)
            }.getOrThrow()
        } catch (e: Exception) {
            Timber.e(e, "gdrive upload of '$targetFileName' failed")
            throw e
        }
}