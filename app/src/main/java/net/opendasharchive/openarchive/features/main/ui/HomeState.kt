package net.opendasharchive.openarchive.features.main.ui

import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Vault

/**
 * Activity-scoped state for the Home shell.
 * This is the SINGLE SOURCE OF TRUTH for:
 * - All spaces
 * - Current selected space
 * - All projects in the current space
 * - Currently selected project ID
 * - Pager state
 *
 * MainMediaViewModel should NOT duplicate this data.
 */
data class HomeState(
    val spaces: List<Vault> = emptyList(),
    val currentSpace: Vault? = null,
    val projects: List<Archive> = emptyList(),
    val selectedProjectId: Long? = null,
    val pagerIndex: Int = 0,
    val lastMediaIndex: Int = 0,
    val showContentPicker: Boolean = false,
    val showUploadManager: Boolean = false,
    val mediaRefreshProjectId: Long? = null,
    val mediaRefreshToken: Long = 0L
)