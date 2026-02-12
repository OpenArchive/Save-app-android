package net.opendasharchive.openarchive.services.snowbird

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.EvidenceDao
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.data.toDwebEntity
import net.opendasharchive.openarchive.services.snowbird.data.toDomain
import net.opendasharchive.openarchive.services.snowbird.data.toEvidenceEntity
import net.opendasharchive.openarchive.services.snowbird.data.*
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdFileRepository {
    suspend fun fetchFiles(archiveId: Long, groupKey: String, repoKey: String, forceRefresh: Boolean = false): SnowbirdResult<Unit>
    fun observeFiles(archiveId: Long): Flow<List<Evidence>>
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): SnowbirdResult<ByteArray>
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): SnowbirdResult<FileUploadResult>
}

class SnowbirdFileRepository(
    private val api: ISnowbirdAPI,
    private val evidenceDao: EvidenceDao,
    private val dwebDao: DwebDao
) : ISnowbirdFileRepository {

    override fun observeFiles(archiveId: Long): Flow<List<Evidence>> {
        return dwebDao.observeEvidenceWithDweb(archiveId).map { evidenceList ->
            evidenceList.map { it.toDomain() }
        }
    }

    override suspend fun fetchFiles(archiveId: Long, groupKey: String, repoKey: String, forceRefresh: Boolean): SnowbirdResult<Unit> {
        return try {
            val response = api.fetchFiles(groupKey, repoKey)
            response.files.forEach { fileDto ->
                val evidenceEntity = fileDto.toEvidenceEntity(archiveId)
                val evidenceId = evidenceDao.upsert(evidenceEntity)
                val dwebEntity = fileDto.toDwebEntity(evidenceId)
                dwebDao.upsertEvidenceMetadata(dwebEntity)
            }
            SnowbirdResult.Success(Unit)
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