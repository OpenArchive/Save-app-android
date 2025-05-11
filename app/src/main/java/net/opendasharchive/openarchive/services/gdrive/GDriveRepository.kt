package net.opendasharchive.openarchive.services.gdrive

import com.google.api.client.http.InputStreamContent
import com.google.gson.Gson

class GDriveRepository(private val client: GDriveClient, private val gson: Gson = Gson()) {

    suspend fun newFolder(name: String, parent: String? = null): Result<GDriveFile> = try {
        client.newFolder(name, parent).map {
            gson.fromJson(it, GDriveFile::class.java)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun folders(parents: String? = null, pageSize: Int = 1000, pageToken: String? = null): Result<GDriveFileList> = try {
        client.listFolders(parents, pageSize, pageToken).map {
            gson.fromJson(it, GDriveFileList::class.java)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun upload(file: GDriveFile, content: InputStreamContent, onProgress: ProgressListener): Result<GDriveFile> = try {
        client.upload(file, content, onProgress).map {
            gson.fromJson(it, GDriveFile::class.java)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}