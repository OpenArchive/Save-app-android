package net.opendasharchive.openarchive.core.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Archive - Domain representation of a folder or project.
 * (Formerly known as Project)
 */
@Serializable
data class Archive(
    val id: Long = 0L,
    val description: String? = null,
    val created: LocalDateTime? = null,
    val vaultId: Long? = null,
    val isArchived: Boolean = false,
    val openSubmissionId: Long = -1L,
    val licenseUrl: String? = null,
    val isRemote: Boolean = false,
    val archiveKey: String? = null,
    val archiveHash: String? = null,
    val permissions: ArchivePermission? = null
)
