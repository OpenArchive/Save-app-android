package net.opendasharchive.openarchive.services.snowbird.service.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.EvidenceDao
import net.opendasharchive.openarchive.extensions.toDomainError
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.services.snowbird.data.toDwebEntity
import net.opendasharchive.openarchive.services.snowbird.data.toDomain
import net.opendasharchive.openarchive.services.snowbird.data.toEvidenceEntity
import net.opendasharchive.openarchive.services.snowbird.data.*
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdFileRepository {
    suspend fun fetchFiles(archiveId: Long, groupKey: String, repoKey: String, forceRefresh: Boolean = false): DomainResult<Unit>
    fun observeFiles(archiveId: Long): Flow<List<Evidence>>
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): DomainResult<ByteArray>
    suspend fun markFileDownloaded(evidenceId: Long, localFilePath: String, mimeType: String): DomainResult<Unit>
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): DomainResult<FileUploadResult>
}

class SnowbirdFileRepository(
    private val api: ISnowbirdAPI,
    private val evidenceDao: EvidenceDao,
    private val archiveDao: ArchiveDao,
    private val dwebDao: DwebDao
) : ISnowbirdFileRepository {

    override fun observeFiles(archiveId: Long): Flow<List<Evidence>> {
        return dwebDao.observeEvidenceWithDweb(archiveId).map { evidenceList ->
            evidenceList.map { it.toDomain() }
        }
    }

    override suspend fun fetchFiles(archiveId: Long, groupKey: String, repoKey: String, forceRefresh: Boolean): DomainResult<Unit> {
        return try {
            val response = api.fetchFiles(groupKey, repoKey)
            response.files.forEach { fileDto ->
                val archive = archiveDao.getById(archiveId)
                val submissionId = archive?.openSubmissionId ?: 0L
                
                // Deduplication lookup STRICTLY by content hash
                val existingEvidenceId = evidenceDao.getEvidenceIdByHash(archiveId, fileDto.hash)

                // Preserve local file linkage and best-known mime type for existing downloaded items.
                val existingEvidence = existingEvidenceId?.let { evidenceDao.getById(it) }
                val mappedEvidence = fileDto.toEvidenceEntity(
                    archiveId = archiveId,
                    submissionId = submissionId,
                    id = existingEvidenceId ?: 0L
                )
                val preservedMimeType = when {
                    existingEvidence?.mimeType.isNullOrBlank() -> mappedEvidence.mimeType
                    mappedEvidence.mimeType == "application/octet-stream" -> existingEvidence!!.mimeType
                    else -> mappedEvidence.mimeType
                }
                val evidenceEntity = if (existingEvidence != null) {
                    mappedEvidence.copy(
                        originalFilePath = if (existingEvidence.originalFilePath.isNotBlank()) {
                            existingEvidence.originalFilePath
                        } else {
                            mappedEvidence.originalFilePath
                        },
                        mimeType = preservedMimeType
                    )
                } else {
                    mappedEvidence
                }
                val upsertedId = evidenceDao.upsert(evidenceEntity)
                val evidenceId = existingEvidenceId ?: upsertedId

                val existingDweb = dwebDao.getEvidenceWithDwebById(evidenceId)?.dwebMetadata
                val dwebEntity = fileDto.toDwebEntity(evidenceId).copy(
                    isDownloaded = fileDto.isDownloaded || (existingDweb?.isDownloaded == true)
                )
                dwebDao.upsertEvidenceMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): DomainResult<ByteArray> {
        return try {
            val response = api.downloadFile(groupKey, repoKey, filename)
            DomainResult.Success(response)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun markFileDownloaded(
        evidenceId: Long,
        localFilePath: String,
        mimeType: String
    ): DomainResult<Unit> {
        return try {
            dwebDao.markEvidenceDownloaded(
                evidenceId = evidenceId,
                localFilePath = localFilePath,
                mimeType = mimeType,
                updatedAt = DateUtils.nowDateTime
            )
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): DomainResult<FileUploadResult> {
        return try {
            val response = api.uploadFile(groupKey, repoKey, uri)
            DomainResult.Success(response)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

}
