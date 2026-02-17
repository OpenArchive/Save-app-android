package net.opendasharchive.openarchive.services.snowbird

import android.net.Uri
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.toFile
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService

interface ISnowbirdFileRepository {
    suspend fun fetchFiles(groupKey: String, repoKey: String, forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdFileItem>>
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): SnowbirdResult<ByteArray>
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): SnowbirdResult<FileUploadResult>
}

class SnowbirdFileRepository(val api: ISnowbirdAPI) : ISnowbirdFileRepository {

    private fun ensureServerReadyForNetwork(): SnowbirdResult.Error? {
        return if (SnowbirdService.getCurrentStatus() is ServiceStatus.Connected) {
            null
        } else {
            SnowbirdResult.Error(SnowbirdError.GeneralError("DWeb server is not ready. Enable DWeb Server and wait for it to connect."))
        }
    }

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
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.fetchFiles(groupKey, repoKey)
            val files = response.files.map { it.toFile(groupKey = groupKey, repoKey = repoKey) }
            SnowbirdResult.Success(files)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): SnowbirdResult<ByteArray> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.downloadFile(groupKey, repoKey, filename)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): SnowbirdResult<FileUploadResult> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.uploadFile(groupKey, repoKey, uri)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

}