package net.opendasharchive.openarchive.core.domain.mappers

import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.db.*
import net.opendasharchive.openarchive.util.*

/**
 * Extension mappers between Clean domain models and Room Entities.
 */

// --- Room Entity Mappers ---

fun VaultEntity.toDomain(): Vault = Vault(
    id = this.id,
    type = when (this.type) {
        0 -> VaultType.PRIVATE_SERVER
        1 -> VaultType.INTERNET_ARCHIVE
        5 -> VaultType.DWEB_STORAGE
        else -> VaultType.PRIVATE_SERVER
    },
    name = this.name,
    username = this.username,
    displayName = this.displayname,
    password = this.password,
    host = this.host,
    metaData = this.metaData,
    licenseUrl = this.licenseUrl
)

fun Vault.toVaultEntity(): VaultEntity = VaultEntity(
    id = this.id,
    type = when (this.type) {
        VaultType.PRIVATE_SERVER -> 0
        VaultType.INTERNET_ARCHIVE -> 1
        VaultType.DWEB_STORAGE -> 5
    },
    name = this.name,
    username = this.username,
    displayname = this.displayName,
    password = this.password,
    host = this.host,
    metaData = this.metaData,
    licenseUrl = this.licenseUrl
)

fun ArchiveEntity.toDomain(): Archive = Archive(
    id = this.id,
    description = this.description,
    created = this.created?.toLocalDateTime(),
    vaultId = this.spaceId,
    isArchived = this.archived,
    licenseUrl = this.licenseUrl
)

fun Archive.toArchiveEntity(): ArchiveEntity = ArchiveEntity(
    id = this.id,
    description = this.description,
    created = this.created?.toEpochMilliseconds(),
    spaceId = this.vaultId ?: 0L,
    archived = this.isArchived,
    openCollectionId = -1, // This needs to be managed by the repository/DAO
    licenseUrl = this.licenseUrl
)

fun SubmissionEntity.toDomain(): Submission = Submission(
    id = this.id,
    archiveId = this.projectId,
    vaultId = 0L, // Will be resolved by repository if needed
    uploadDate = this.uploadDate?.toLocalDateTime(),
    serverUrl = this.serverUrl
)

fun Submission.toSubmissionEntity(): SubmissionEntity = SubmissionEntity(
    id = this.id,
    projectId = this.archiveId,
    uploadDate = this.uploadDate?.toEpochMilliseconds(),
    serverUrl = this.serverUrl
)

fun EvidenceEntity.toDomain(vaultId: Long = 0L): Evidence = Evidence(
    id = this.id,
    originalFilePath = this.originalFilePath,
    mimeType = this.mimeType,
    createDate = this.createDate?.toLocalDateTime(),
    updateDate = this.updateDate?.toLocalDateTime(),
    uploadDate = this.uploadDate?.toLocalDateTime(),
    serverUrl = this.serverUrl,
    title = this.title,
    description = this.description,
    author = this.author,
    location = this.location,
    tags = if (this.tags.isBlank()) emptyList() else this.tags.split(";"),
    licenseUrl = this.licenseUrl,
    mediaHash = this.mediaHash,
    mediaHashString = this.mediaHashString,
    status = when (this.status) {
        0 -> EvidenceStatus.NEW
        1 -> EvidenceStatus.LOCAL
        2 -> EvidenceStatus.QUEUED
        4 -> EvidenceStatus.UPLOADING
        5 -> EvidenceStatus.UPLOADED
        9 -> EvidenceStatus.ERROR
        else -> EvidenceStatus.NEW
    },
    statusMessage = this.statusMessage,
    vaultId = vaultId, // Resolved by repository
    archiveId = this.projectId,
    submissionId = this.collectionId,
    contentLength = this.contentLength,
    progress = this.progress,
    uploadPercentage = if (this.contentLength > 0) (this.progress.toFloat() / this.contentLength * 100).toInt() else null,
    isFlagged = this.flag,
    priority = this.priority,
    isSelected = false // UI only
)

fun Evidence.toEvidenceEntity(): EvidenceEntity = EvidenceEntity(
    id = this.id,
    originalFilePath = this.originalFilePath,
    mimeType = this.mimeType,
    createDate = this.createDate?.toEpochMilliseconds(),
    updateDate = this.updateDate?.toEpochMilliseconds(),
    uploadDate = this.uploadDate?.toEpochMilliseconds(),
    serverUrl = this.serverUrl,
    title = this.title,
    description = this.description,
    author = this.author,
    location = this.location,
    tags = this.tags.joinToString(";"),
    licenseUrl = this.licenseUrl,
    mediaHash = this.mediaHash,
    mediaHashString = this.mediaHashString,
    status = when (this.status) {
        EvidenceStatus.NEW -> 0
        EvidenceStatus.LOCAL -> 1
        EvidenceStatus.QUEUED -> 2
        EvidenceStatus.UPLOADING -> 4
        EvidenceStatus.UPLOADED -> 5
        EvidenceStatus.ERROR -> 9
    },
    statusMessage = this.statusMessage,
    projectId = this.archiveId,
    collectionId = this.submissionId,
    contentLength = this.contentLength,
    progress = this.progress,
    flag = this.isFlagged,
    priority = this.priority
)
