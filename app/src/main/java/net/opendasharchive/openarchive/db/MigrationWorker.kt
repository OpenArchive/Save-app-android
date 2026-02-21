package net.opendasharchive.openarchive.db

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orm.SugarRecord
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.security.VaultCredentialStore
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.Prefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.db.sugar.Collection as SugarCollection
import net.opendasharchive.openarchive.db.sugar.Media as SugarMedia
import net.opendasharchive.openarchive.db.sugar.Project as SugarProject
import net.opendasharchive.openarchive.db.sugar.Space as SugarSpace
import net.opendasharchive.openarchive.util.toLocalDateTime

class MigrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val vaultDao: VaultDao by inject()
    private val archiveDao: ArchiveDao by inject()
    private val submissionDao: SubmissionDao by inject()
    private val evidenceDao: EvidenceDao by inject()
    private val migrationDao: MigrationDao by inject()
    private val credentialStore: VaultCredentialStore by inject()

    override suspend fun doWork(): Result {
        if (Prefs.isRoomMigrated) return Result.success()

        Prefs.isMigrationInProgress = true

        try {
            var state = migrationDao.getMigrationState() ?: MigrationStateEntity(
                stage = "IDLE",
                processedCount = 0,
                totalCount = 0
            )

            if (state.stage == "IDLE" || state.stage == "SPACES") {
                migrateSpaces()
                state = migrationDao.getMigrationState()!!
            }

            if (state.stage == "PROJECTS") {
                migrateProjects()
                state = migrationDao.getMigrationState()!!
            }

            if (state.stage == "COLLECTIONS") {
                migrateCollections()
                state = migrationDao.getMigrationState()!!
            }

            if (state.stage == "MEDIA") {
                migrateMedia()
                state = migrationDao.getMigrationState()!!
            }

            migrationDao.upsert(state.copy(stage = "DONE", completedAt = DateUtils.now))

            Prefs.isRoomMigrated = true
            Prefs.isMigrationInProgress = false
            AppLogger.i("Migration to Room completed successfully")
            return Result.success()
        } catch (e: Exception) {
            AppLogger.e("Migration to Room failed", e)
            Prefs.isMigrationInProgress = false
            return Result.retry()
        }
    }

    private suspend fun migrateSpaces() {
        val spaces = SugarSpace.getAll().asSequence().toList()
        AppLogger.i("Migrating ${spaces.size} spaces")

        spaces.forEach { space ->
            val vaultId = vaultDao.upsert(
                VaultEntity(
                    id = space.id,
                    type = when (space.tType) {
                        SugarSpace.Type.WEBDAV -> VaultType.PRIVATE_SERVER
                        SugarSpace.Type.INTERNET_ARCHIVE -> VaultType.INTERNET_ARCHIVE
                        SugarSpace.Type.RAVEN -> VaultType.DWEB_STORAGE
                    },
                    name = space.name,
                    username = space.username,
                    displayName = space.displayname,
                    host = space.host,
                    metaData = space.metaData,
                    licenseUrl = space.license,
                    createdAt = DateUtils.now.toLocalDateTime()
                )
            )
            if (space.password.isNotBlank()) {
                credentialStore.putSecret(vaultId, space.password)
            }
        }
        migrationDao.upsert(MigrationStateEntity(stage = "PROJECTS", processedCount = 0, totalCount = 0))
    }

    private suspend fun migrateProjects() {
        val projects = SugarRecord.findAll(SugarProject::class.java).asSequence().toList()
        AppLogger.i("Migrating ${projects.size} projects")

        projects.forEach { project ->
            archiveDao.upsert(
                ArchiveEntity(
                    id = project.id,
                    description = project.description,
                    createdAt = project.created?.time?.toLocalDateTime(),
                    vaultId = project.spaceId ?: -1,
                    archived = project.isArchived,
                    openSubmissionId = project.openCollectionId,
                    licenseUrl = project.licenseUrl,
                    isRemote = false
                )
            )
        }
        migrationDao.upsert(MigrationStateEntity(stage = "COLLECTIONS", processedCount = 0, totalCount = 0))
    }

    private suspend fun migrateCollections() {
        val collections = SugarRecord.findAll(SugarCollection::class.java).asSequence().toList()
        AppLogger.i("Migrating ${collections.size} collections")

        collections.forEach { collection ->
            submissionDao.upsert(
                SubmissionEntity(
                    id = collection.id,
                    archiveId = collection.projectId ?: -1,
                    uploadedAt = collection.uploadDate?.time?.toLocalDateTime(),
                    serverUrl = collection.serverUrl
                )
            )
        }
        migrationDao.upsert(MigrationStateEntity(stage = "MEDIA", processedCount = 0, totalCount = 0))
    }

    private suspend fun migrateMedia() {
        val mediaList = SugarRecord.findAll(SugarMedia::class.java).asSequence().toList()
        AppLogger.i("Migrating ${mediaList.size} media items")

        mediaList.forEach { media ->
            evidenceDao.upsert(
                EvidenceEntity(
                    id = media.id,
                    originalFilePath = media.originalFilePath,
                    mimeType = media.mimeType,
                    createdAt = media.createDate?.time?.toLocalDateTime(),
                    updatedAt = media.updateDate?.time?.toLocalDateTime(),
                    uploadedAt = media.uploadDate?.time?.toLocalDateTime(),
                    serverUrl = media.serverUrl,
                    title = media.title,
                    description = media.description,
                    author = media.author,
                    location = media.location,
                    tags = media.tags,
                    licenseUrl = media.licenseUrl,
                    mediaHashString = media.mediaHashString,
                    status = when (media.sStatus) {
                        SugarMedia.Status.Local -> EvidenceStatus.LOCAL
                        SugarMedia.Status.Queued -> EvidenceStatus.QUEUED
                        SugarMedia.Status.Uploading -> EvidenceStatus.UPLOADING
                        SugarMedia.Status.Uploaded -> EvidenceStatus.UPLOADED
                        SugarMedia.Status.Error -> EvidenceStatus.ERROR
                        else -> EvidenceStatus.NEW
                    },
                    statusMessage = media.statusMessage,
                    archiveId = media.projectId,
                    submissionId = media.collectionId,
                    contentLength = media.contentLength,
                    progress = media.progress,
                    flag = media.flag,
                    priority = media.priority
                )
            )
        }
    }
}
