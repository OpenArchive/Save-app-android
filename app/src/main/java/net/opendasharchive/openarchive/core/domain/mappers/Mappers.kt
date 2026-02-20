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
    type = this.type,
    name = this.name,
    username = this.username,
    displayName = this.displayName,
    password = this.password,
    host = this.host,
    metaData = this.metaData,
    licenseUrl = this.licenseUrl,
    createdAt = this.createdAt
)

fun VaultWithDweb.toDomain(): Vault = vault.toDomain().copy(
    vaultKey = dwebMetadata?.vaultKey
)

fun Vault.toVaultEntity(): VaultEntity = VaultEntity(
    id = this.id,
    type = this.type,
    name = this.name,
    username = this.username,
    displayName = this.displayName,
    password = this.password,
    host = this.host,
    metaData = this.metaData,
    licenseUrl = this.licenseUrl,
    createdAt = this.createdAt ?: DateUtils.now.toLocalDateTime()
)

fun Vault.toDwebEntity(): VaultDwebEntity? = vaultKey?.let {
    VaultDwebEntity(
        vaultId = id,
        vaultKey = it
    )
}

fun ArchiveEntity.toDomain(): Archive = Archive(
    id = this.id,
    description = this.description,
    created = this.createdAt,
    vaultId = this.vaultId,
    isArchived = this.archived,
    openSubmissionId = this.openSubmissionId,
    licenseUrl = this.licenseUrl,
    isRemote = this.isRemote
)

fun ArchiveWithDweb.toDomain(): Archive = archive.toDomain().copy(
    archiveKey = dwebMetadata?.archiveKey,
    archiveHash = dwebMetadata?.archiveHash,
    permissions = dwebMetadata?.permissions
)

fun Archive.toArchiveEntity(): ArchiveEntity = ArchiveEntity(
    id = this.id,
    description = this.description,
    createdAt = this.created,
    vaultId = this.vaultId ?: 0L,
    archived = this.isArchived,
    openSubmissionId = this.openSubmissionId,
    licenseUrl = this.licenseUrl,
    isRemote = this.isRemote
)

fun Archive.toDwebEntity(): ArchiveDwebEntity? =
    if (archiveKey != null && archiveHash != null && permissions != null) {
        ArchiveDwebEntity(
            archiveId = id,
            archiveKey = archiveKey,
            archiveHash = archiveHash,
            permissions = permissions
        )
    } else null

fun SubmissionEntity.toDomain(): Submission = Submission(
    id = this.id,
    archiveId = this.archiveId,
    vaultId = 0L, // Will be resolved by repository
    uploadDate = this.uploadedAt,
    serverUrl = this.serverUrl
)

fun Submission.toSubmissionEntity(): SubmissionEntity = SubmissionEntity(
    id = this.id,
    archiveId = this.archiveId,
    uploadedAt = this.uploadDate,
    serverUrl = this.serverUrl
)

fun EvidenceEntity.toDomain(vaultId: Long = 0L): Evidence = Evidence(
    id = this.id,
    originalFilePath = this.originalFilePath,
    mimeType = this.mimeType,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    uploadedAt = this.uploadedAt,
    serverUrl = this.serverUrl,
    title = this.title,
    description = this.description,
    author = this.author,
    location = this.location,
    tags = if (this.tags.isBlank()) emptyList() else this.tags.split(";"),
    licenseUrl = this.licenseUrl,
    mediaHashString = this.mediaHashString,
    status = this.status,
    statusMessage = this.statusMessage,
    vaultId = vaultId,
    archiveId = this.archiveId,
    submissionId = this.submissionId,
    contentLength = this.contentLength,
    progress = this.progress,
    uploadPercentage = if (this.contentLength > 0) (this.progress.toFloat() / this.contentLength * 100).toInt() else null,
    isFlagged = this.flag,
    priority = this.priority,
    isSelected = false // UI only
)

fun EvidenceWithDweb.toDomain(vaultId: Long = 0L): Evidence = evidence.toDomain(vaultId).copy(
    isDownloaded = dwebMetadata?.isDownloaded ?: false
)

fun Evidence.toEvidenceEntity(): EvidenceEntity = EvidenceEntity(
    id = this.id,
    originalFilePath = this.originalFilePath,
    mimeType = this.mimeType,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    uploadedAt = this.uploadedAt,
    serverUrl = this.serverUrl,
    title = this.title,
    description = this.description,
    author = this.author,
    location = this.location,
    tags = this.tags.joinToString(";"),
    licenseUrl = this.licenseUrl,
    mediaHashString = this.mediaHashString,
    status = this.status,
    statusMessage = this.statusMessage,
    archiveId = this.archiveId,
    submissionId = this.submissionId,
    contentLength = this.contentLength,
    progress = this.progress,
    flag = this.isFlagged,
    priority = this.priority
)

fun Evidence.toDwebEntity(): EvidenceDwebEntity = EvidenceDwebEntity(
    evidenceId = id,
    isDownloaded = isDownloaded
)
