package net.opendasharchive.openarchive.services.snowbird.service.repository

import android.content.res.AssetManager
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.DomainError
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.db.*
import net.opendasharchive.openarchive.services.snowbird.data.*
import net.opendasharchive.openarchive.util.DateUtils

class MockSnowbirdGroupRepository(
    private val assetManager: AssetManager,
    private val config: AppConfig,
    private val vaultDao: VaultDao,
    private val dwebDao: DwebDao
) : ISnowbirdGroupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createGroup(groupName: String): DomainResult<Vault> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Server("Simulated error creating group"))
        
        return try {
            val content = assetManager.open("dweb/dweb_create_group_response.json").bufferedReader().use { it.readText() }
            val dto = json.decodeFromString<SnowbirdGroupDTO>(content)
            
            // Persist to Room
            val vaultEntity = dto.toVaultEntity()
            val vaultId = vaultDao.upsert(vaultEntity)
            val dwebEntity = dto.toDwebEntity(vaultId)
            dwebDao.upsertVaultMetadata(dwebEntity)
            
            val savedVaultWithDweb = dwebDao.getVaultWithDwebById(vaultId)
            val savedVault = savedVaultWithDweb?.toDomain()
            
            if (savedVault != null) {
                DomainResult.Success(savedVault)
            } else {
                DomainResult.Error(DomainError.Unknown("Failed to retrieve saved mock group"))
            }
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown("Failed to parse mock data: ${e.message}"))
        }
    }

    override suspend fun fetchGroups(forceRefresh: Boolean): DomainResult<Unit> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Network("Simulated network error fetching groups"))
        
        return try {
            val content = assetManager.open("dweb/dweb_fetch_groups_response.json").bufferedReader().use { it.readText() }
            val dtos = json.decodeFromString<SnowbirdGroupListDTO>(content).groups
            
            dtos.forEach { dto ->
                // Deduplication lookup STRICTLY by DWeb key
                val existingVaultId = dwebDao.getVaultIdByKey(dto.key)
                
                // Map using the fixed ID (if exists) or 0 (for new)
                val vaultEntity = dto.toVaultEntity(id = existingVaultId ?: 0L)
                val upsertedId = vaultDao.upsert(vaultEntity)
                val vaultId = existingVaultId ?: upsertedId
                
                val dwebEntity = dto.toDwebEntity(vaultId)
                dwebDao.upsertVaultMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            DomainResult.Error(DomainError.Unknown("Failed to parse mock data: ${e.message}"))
        }
    }

    override fun observeGroups(): Flow<List<Vault>> {
        return dwebDao.observeVaultsWithDweb().map { vaultList ->
            vaultList.map { it.toDomain() }
        }
    }

    override suspend fun getVaultIdByKey(groupKey: String): Long? {
        return dwebDao.getVaultIdByKey(groupKey)
    }

    override suspend fun joinGroup(uriString: String): DomainResult<Vault> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Unknown("Simulated error joining group"))

        val content = assetManager.open("dweb/dweb_create_group_response.json").bufferedReader().use { it.readText() }
        val groupDto = json.decodeFromString<SnowbirdGroupDTO>(content).copy(
            uri = uriString
        )

        val existingVaultId = dwebDao.getVaultIdByKey(groupDto.key)
        val vaultEntity = groupDto.toVaultEntity(id = existingVaultId ?: 0L)
        val upsertedId = vaultDao.upsert(vaultEntity)
        val vaultId = existingVaultId ?: upsertedId
        dwebDao.upsertVaultMetadata(groupDto.toDwebEntity(vaultId))

        val savedVaultWithDweb = dwebDao.getVaultWithDwebById(vaultId)
        val savedVault = savedVaultWithDweb?.toDomain()

        return if (savedVault != null) {
            DomainResult.Success(savedVault)
        } else {
            DomainResult.Error(DomainError.Unknown("Failed to retrieve saved mock group"))
        }
    }
}

class MockSnowbirdRepoRepository(
    private val assetManager: AssetManager,
    private val config: AppConfig,
    private val archiveDao: ArchiveDao,
    private val submissionDao: SubmissionDao,
    private val dwebDao: DwebDao
) : ISnowbirdRepoRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createRepo(vaultId: Long, groupKey: String, repoName: String): DomainResult<Archive> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Server("Simulated error creating repo"))

        return try {
            val content = assetManager.open("dweb/dweb_create_repo_response.json").bufferedReader().use { it.readText() }
            val dto = json.decodeFromString<SnowbirdRepoDTO>(content)
            
            // First lookup by key
            val existingArchiveId = dwebDao.getArchiveIdByKey(dto.key)
            
            // 1. Upsert Archive FIRST with placeholder submissionId to get its internal ID
            val archiveEntityPlaceholder = dto.toArchiveEntity(vaultId, 0, id = existingArchiveId ?: 0L)
            val upsertedArchiveId = archiveDao.upsert(archiveEntityPlaceholder)
            val archiveId = existingArchiveId ?: upsertedArchiveId
            
            // 2. Now we have a valid archiveId, so we can create/update the Submission
            val archive = archiveDao.getById(archiveId)
            var submissionId = archive?.openSubmissionId ?: 0L
            if (submissionId == 0L) {
                submissionId = submissionDao.upsert(SubmissionEntity(archiveId = archiveId, uploadedAt = null, serverUrl = null))
            }
            
            // 3. Finally update Archive with the real submissionId
            archiveDao.upsert(dto.toArchiveEntity(vaultId, submissionId, id = archiveId))
            
            val dwebEntity = dto.toDwebEntity(archiveId)
            dwebDao.upsertArchiveMetadata(dwebEntity)
            
            val savedArchiveWithDweb = dwebDao.getArchiveWithDwebById(archiveId)
            val savedArchive = savedArchiveWithDweb?.toDomain()
            
            if (savedArchive != null) {
                DomainResult.Success(savedArchive)
            } else {
                DomainResult.Error(DomainError.Unknown("Failed to retrieve saved mock repo"))
            }
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown("Failed to parse mock data: ${e.message}"))
        }
    }

    override suspend fun fetchRepos(vaultId: Long, groupKey: String, forceRefresh: Boolean): DomainResult<Unit> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Network("Simulated error fetching repos"))
        
        return try {
            val content = assetManager.open("dweb/dweb_fetch_repos_response.json").bufferedReader().use { it.readText() }
            val dtos = json.decodeFromString<SnowbirdRepoListDTO>(content).repos
            
            dtos.forEach { dto ->
                // Deduplication lookup STRICTLY by DWeb key
                val existingArchiveId = dwebDao.getArchiveIdByKey(dto.key)
                
                // If not found by key, fallback to finding by name (legacy or first-time sync)
                val fallbackId = archiveDao.getByName(vaultId, dto.name ?: "")?.id
                val currentId = existingArchiveId ?: fallbackId
                
                // 1. Upsert archive with placeholder to ensure it exists
                val upsertedArchiveId = archiveDao.upsert(dto.toArchiveEntity(vaultId, 0, id = currentId ?: 0L))
                val archiveId = currentId ?: upsertedArchiveId
                
                // 2. Ensure there's an open submission for this archive
                val archive = archiveDao.getById(archiveId)
                var submissionId = archive?.openSubmissionId ?: 0L
                if (submissionId == 0L) {
                    submissionId = submissionDao.upsert(SubmissionEntity(archiveId = archiveId, uploadedAt = null, serverUrl = null))
                }
                
                // 3. Now update with the correct submissionId and ID
                archiveDao.upsert(dto.toArchiveEntity(vaultId, submissionId, id = archiveId))
                
                val dwebEntity = dto.toDwebEntity(archiveId)
                dwebDao.upsertArchiveMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            DomainResult.Error(DomainError.Unknown("Failed to parse mock data: ${e.message}"))
        }
    }

    override fun observeRepos(vaultId: Long, archived: Boolean): Flow<List<Archive>> {
        return dwebDao.observeArchivesWithDweb(vaultId, archived).map { archiveList ->
            archiveList.map { it.toDomain() }
        }
    }

    override suspend fun refreshGroupContent(groupKey: String): DomainResult<RefreshGroupResponse> {
        delay(config.mockDelayMs)
        return DomainResult.Success(RefreshGroupResponse(success = true))
    }

    private suspend fun apiCreateRepoLikeBehavior(groupKey: String, repoName: String) {
        assetManager.open("dweb/dweb_create_repo_response.json").bufferedReader().use { it.readText() }
    }
}

class MockSnowbirdFileRepository(
    private val assetManager: AssetManager,
    private val config: AppConfig,
    private val evidenceDao: EvidenceDao,
    private val archiveDao: ArchiveDao,
    private val dwebDao: DwebDao
) : ISnowbirdFileRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchFiles(archiveId: Long, groupKey: String, repoKey: String, forceRefresh: Boolean): DomainResult<Unit> {
        delay(config.mockDelayMs)
        if (config.simulateErrors) return DomainResult.Error(DomainError.Network("Simulated error fetching files"))
        
        return try {
            val content = assetManager.open("dweb/dweb_fetch_medias_response.json").bufferedReader().use { it.readText() }
            val dtos = json.decodeFromString<SnowbirdFileListDTO>(content).files
            
            dtos.forEach { dto ->
                val archive = archiveDao.getById(archiveId)
                val submissionId = archive?.openSubmissionId ?: 0L
                
                // Deduplication lookup STRICTLY by content hash
                val existingEvidenceId = evidenceDao.getEvidenceIdByHash(archiveId, dto.hash)
                
                // Map using the fixed ID (if exists) or 0 (for new)
                val evidenceEntity = dto.toEvidenceEntity(archiveId, submissionId, id = existingEvidenceId ?: 0L)
                val upsertedId = evidenceDao.upsert(evidenceEntity)
                val evidenceId = existingEvidenceId ?: upsertedId
                
                val dwebEntity = dto.toDwebEntity(evidenceId)
                dwebDao.upsertEvidenceMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            DomainResult.Error(DomainError.Unknown("Failed to parse mock data: ${e.message}"))
        }
    }

    override fun observeFiles(archiveId: Long): Flow<List<Evidence>> {
        return dwebDao.observeEvidenceWithDweb(archiveId).map { evidenceList ->
            evidenceList.map { it.toDomain() }
        }
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): DomainResult<ByteArray> {
        delay(config.mockDelayMs)
        return DomainResult.Success(ByteArray(0))
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
            DomainResult.Error(DomainError.Unknown("Failed to mark file as downloaded: ${e.message}"))
        }
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): DomainResult<FileUploadResult> {
        delay(config.mockDelayMs)
        return DomainResult.Success(FileUploadResult(name = "mock_file", updatedCollectionHash = "mock_hash"))
    }
}
