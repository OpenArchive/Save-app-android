package net.opendasharchive.openarchive.services.snowbird

import android.net.Uri
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdFileRepository {
    suspend fun fetchFiles(groupKey: String, repoKey: String, forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdFileItem>>
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): SnowbirdResult<ByteArray>
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): SnowbirdResult<FileUploadResult>
}

class SnowbirdFileRepository(val api: ISnowbirdAPI) : ISnowbirdFileRepository {
    override suspend fun fetchFiles(groupKey: String, repoKey: String, forceRefresh: Boolean): SnowbirdResult<List<SnowbirdFileItem>> {
        return if (forceRefresh) {
            fetchFilesFromNetwork(groupKey, repoKey)
        } else {
            fetchFilesFromCache(groupKey, repoKey)
        }
    }

    private fun fetchFilesFromCache(groupKey: String, repoKey: String): SnowbirdResult<List<SnowbirdFileItem>> {
        return SnowbirdResult.Success(SnowbirdFileItem.findBy(groupKey, repoKey))
    }

    private suspend fun fetchFilesFromNetwork(groupKey: String, repoKey: String): SnowbirdResult<List<SnowbirdFileItem>> {
        return try {
            val response = api.fetchFiles(groupKey, repoKey)
            SnowbirdResult.Success(response.files)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): SnowbirdResult<ByteArray> {
        return try {
            val response = api.downloadFile(groupKey, repoKey, filename)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): SnowbirdResult<FileUploadResult> {
        return try {
            val response = api.uploadFile(groupKey, repoKey, uri)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

}