package net.opendasharchive.openarchive.core.domain.mappers

import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.db.sugar.Media
import net.opendasharchive.openarchive.db.sugar.Project
import net.opendasharchive.openarchive.db.sugar.Collection as SugarCollection
import net.opendasharchive.openarchive.db.sugar.Space
import net.opendasharchive.openarchive.util.*

/**
 * Extension mappers between SugarORM entities and clean domain models.
 */

// --- Vault / Space ---

fun Space.toDomain(): Vault = Vault(
    id = this.id,
    type = when (this.tType) {
        Space.Type.WEBDAV -> VaultType.PRIVATE_SERVER
        Space.Type.INTERNET_ARCHIVE -> VaultType.INTERNET_ARCHIVE
        Space.Type.RAVEN -> VaultType.DWEB_STORAGE
    },
    name = this.name,
    username = this.username,
    displayName = this.displayname,
    password = this.password,
    host = this.host,
    metaData = this.metaData,
    licenseUrl = this.license,
    createdAt = DateUtils.now.toLocalDateTime()
)

fun Vault.toEntity(): Space {
    val space = Space()
    if (this.id != 0L) space.id = this.id
    space.tType = when (this.type) {
        VaultType.PRIVATE_SERVER -> Space.Type.WEBDAV
        VaultType.INTERNET_ARCHIVE -> Space.Type.INTERNET_ARCHIVE
        VaultType.DWEB_STORAGE -> Space.Type.RAVEN
    }
    space.name = this.name
    space.username = this.username
    space.displayname = this.displayName
    space.password = this.password
    space.host = this.host
    space.metaData = this.metaData
    space.license = this.licenseUrl
    return space
}

// --- Archive / Project ---

fun Project.toDomain(): Archive = Archive(
    id = this.id ?: 0L,
    description = this.description,
    created = this.created?.toKotlinLocalDateTime(),
    vaultId = this.spaceId,
    isArchived = this.isArchived,
    openSubmissionId = this.openCollectionId,
    licenseUrl = this.licenseUrl,
    isRemote = false
)

fun Archive.toEntity(): Project {
    val project = Project()
    if (this.id != 0L) project.id = this.id
    project.description = this.description
    project.created = this.created?.toJavaDate()
    project.spaceId = this.vaultId
    project.isArchived = this.isArchived
    project.openCollectionId = this.openSubmissionId
    project.licenseUrl = this.licenseUrl
    return project
}

// --- Submission / Collection ---

fun SugarCollection.toDomain(): Submission = Submission(
    id = this.id ?: 0L,
    archiveId = this.projectId ?: 0L,
    vaultId = Project.getById(this.projectId)?.spaceId ?: 0L,
    uploadDate = this.uploadDate?.toKotlinLocalDateTime(),
    serverUrl = this.serverUrl
)

fun Submission.toEntity(): SugarCollection {
    val collection = SugarCollection()
    if (this.id != 0L) collection.id = this.id
    collection.projectId = this.archiveId
    collection.uploadDate = this.uploadDate?.toJavaDate()
    collection.serverUrl = this.serverUrl
    return collection
}

// --- Evidence / Media ---

fun Media.toDomain(): Evidence = Evidence(
    id = this.id ?: 0L,
    originalFilePath = this.originalFilePath,
    thumbnail = null,
    mimeType = this.mimeType,
    createdAt = this.createDate?.toKotlinLocalDateTime(),
    updatedAt = this.updateDate?.toKotlinLocalDateTime(),
    uploadedAt = this.uploadDate?.toKotlinLocalDateTime(),
    serverUrl = this.serverUrl,
    title = this.title,
    description = this.description,
    author = this.author,
    location = this.location,
    tags = if (this.tags.isBlank()) emptyList() else this.tags.split(";"),
    licenseUrl = this.licenseUrl,
    mediaHashString = this.mediaHashString,
    status = when (this.sStatus) {
        Media.Status.New -> EvidenceStatus.NEW
        Media.Status.Local -> EvidenceStatus.LOCAL
        Media.Status.Queued -> EvidenceStatus.QUEUED
        Media.Status.Uploading -> EvidenceStatus.UPLOADING
        Media.Status.Uploaded -> EvidenceStatus.UPLOADED
        Media.Status.Published -> EvidenceStatus.UPLOADED
        Media.Status.Error -> EvidenceStatus.ERROR
        else -> EvidenceStatus.NEW
    },
    statusMessage = this.statusMessage,
    vaultId = this.space?.id ?: 0L,
    archiveId = this.projectId,
    submissionId = this.collectionId,
    contentLength = this.contentLength,
    progress = this.progress,
    uploadPercentage = if (this.contentLength > 0) (this.progress.toFloat() / this.contentLength * 100).toInt() else null,
    isFlagged = this.flag,
    priority = this.priority,
    isSelected = this.selected
)

fun Evidence.toEntity(): Media {
    val media = Media()
    if (this.id != 0L) media.id = this.id
    media.originalFilePath = this.originalFilePath
    media.mimeType = this.mimeType
    media.createDate = this.createdAt?.toJavaDate()
    media.updateDate = this.updatedAt?.toJavaDate()
    media.uploadDate = this.uploadedAt?.toJavaDate()
    media.serverUrl = this.serverUrl
    media.title = this.title
    media.description = this.description
    media.author = this.author
    media.location = this.location
    media.tags = this.tags.joinToString(";")
    media.licenseUrl = this.licenseUrl
    media.mediaHashString = this.mediaHashString
    media.sStatus = when (this.status) {
        EvidenceStatus.NEW -> Media.Status.New
        EvidenceStatus.LOCAL -> Media.Status.Local
        EvidenceStatus.QUEUED -> Media.Status.Queued
        EvidenceStatus.UPLOADING -> Media.Status.Uploading
        EvidenceStatus.UPLOADED -> Media.Status.Uploaded
        EvidenceStatus.ERROR -> Media.Status.Error
    }
    media.statusMessage = this.statusMessage
    media.projectId = this.archiveId
    media.collectionId = this.submissionId
    media.contentLength = this.contentLength
    media.progress = this.progress
    media.flag = this.isFlagged
    media.priority = this.priority
    media.selected = this.isSelected
    return media
}
