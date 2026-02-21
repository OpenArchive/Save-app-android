package net.opendasharchive.openarchive.services.snowbird.data

import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.ArchivePermission
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.db.ArchiveDwebEntity
import net.opendasharchive.openarchive.db.ArchiveEntity
import net.opendasharchive.openarchive.db.ArchiveWithDweb
import net.opendasharchive.openarchive.db.EvidenceDwebEntity
import net.opendasharchive.openarchive.db.EvidenceEntity
import net.opendasharchive.openarchive.db.EvidenceWithDweb
import net.opendasharchive.openarchive.db.VaultDwebEntity
import net.opendasharchive.openarchive.db.VaultEntity
import net.opendasharchive.openarchive.db.VaultWithDweb
import net.opendasharchive.openarchive.util.DateUtils

fun SnowbirdGroupDTO.toVaultEntity(id: Long = 0): VaultEntity {
    return VaultEntity(
        type = VaultType.DWEB_STORAGE,
        name = name ?: "Untitled Group",
        host = uri ?: "", // Snowbird uses URI as host
        username = "",
        displayName = name ?: "",
        id = id,
        metaData = "",
        licenseUrl = null,
        createdAt = DateUtils.nowDateTime
    )
}

fun SnowbirdGroupDTO.toDwebEntity(vaultId: Long): VaultDwebEntity {
    return VaultDwebEntity(
        vaultId = vaultId,
        vaultKey = key
    )
}

fun VaultWithDweb.toDomain(): Vault {
    return Vault(
        id = vault.id,
        type = vault.type,
        name = vault.name,
        username = vault.username,
        displayName = vault.displayName,
        host = vault.host,
        vaultKey = dwebMetadata?.vaultKey
    )
}

fun SnowbirdRepoDTO.toArchiveEntity(vaultId: Long, submissionId: Long, id: Long = 0): ArchiveEntity {
    return ArchiveEntity(
        id = id,
        vaultId = vaultId,
        description = name ?: "Untitled Repo",
        createdAt = DateUtils.nowDateTime,
        isRemote = true,
        archived = false,
        openSubmissionId = submissionId,
        licenseUrl = null
    )
}

fun SnowbirdRepoDTO.toDwebEntity(archiveId: Long): ArchiveDwebEntity {
    return ArchiveDwebEntity(
        archiveId = archiveId,
        archiveKey = key,
        archiveHash = "", // Initialize as empty
        permissions = ArchivePermission.READ_WRITE
    )
}

fun ArchiveWithDweb.toDomain(): Archive {
    return Archive(
        id = archive.id,
        description = archive.description ?: "",
        created = archive.createdAt ?: DateUtils.nowDateTime,
        vaultId = archive.vaultId,
        licenseUrl = archive.licenseUrl,
        isRemote = archive.isRemote,
        isArchived = archive.archived,
        archiveKey = dwebMetadata?.archiveKey
    )
}

fun SnowbirdFileDTO.toEvidenceEntity(archiveId: Long, submissionId: Long, id: Long = 0): EvidenceEntity {
    return EvidenceEntity(
        id = id,
        originalFilePath = "", // Remote file
        mimeType = mimeType ?: "application/octet-stream",
        createdAt = createdAt?.let { DateUtils.parseDateTime(it) } ?: DateUtils.nowDateTime,
        updatedAt = DateUtils.nowDateTime,
        uploadedAt = DateUtils.nowDateTime,
        serverUrl = "",
        title = name,
        description = "",
        author = "",
        location = "",
        tags = "",
        licenseUrl = null,
        mediaHashString = hash,
        status = EvidenceStatus.UPLOADED,
        statusMessage = "Remote",
        archiveId = archiveId,
        submissionId = submissionId,
        contentLength = size,
        progress = 100,
        flag = false,
        priority = 0
    )
}

fun SnowbirdFileDTO.toDwebEntity(evidenceId: Long): EvidenceDwebEntity {
    return EvidenceDwebEntity(
        evidenceId = evidenceId,
        isDownloaded = false
    )
}

fun EvidenceWithDweb.toDomain(): Evidence {
    return Evidence(
        id = evidence.id,
        originalFilePath = evidence.originalFilePath,
        mimeType = evidence.mimeType,
        createdAt = evidence.createdAt ?: DateUtils.nowDateTime,
        updatedAt = evidence.updatedAt ?: DateUtils.nowDateTime,
        uploadedAt = evidence.uploadedAt,
        serverUrl = evidence.serverUrl,
        title = evidence.title,
        description = evidence.description,
        author = evidence.author,
        location = evidence.location,
        tags = if (evidence.tags.isEmpty()) emptyList() else evidence.tags.split(";"),
        licenseUrl = evidence.licenseUrl,
        mediaHashString = evidence.mediaHashString,
        status = evidence.status,
        statusMessage = evidence.statusMessage,
        archiveId = evidence.archiveId,
        submissionId = evidence.submissionId,
        contentLength = evidence.contentLength,
        progress = evidence.progress,
        isFlagged = evidence.flag,
        priority = evidence.priority,
        isDownloaded = dwebMetadata?.isDownloaded ?: false
    )
}
