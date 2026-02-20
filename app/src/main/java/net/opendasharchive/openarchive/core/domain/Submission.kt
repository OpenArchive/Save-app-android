package net.opendasharchive.openarchive.core.domain

import kotlinx.datetime.LocalDateTime

/**
 * Submission - Domain representation of an upload batch or collection.
 * (Formerly known as Collection)
 */
data class Submission(
    val id: Long = 0L,
    val archiveId: Long = 0L,
    val vaultId: Long = 0L,
    val uploadDate: LocalDateTime? = null,
    val serverUrl: String? = null
)
