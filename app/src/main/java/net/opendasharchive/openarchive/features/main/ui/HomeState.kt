package net.opendasharchive.openarchive.features.main.ui

import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space

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
    val spaces: List<Space> = emptyList(),
    val currentSpace: Space? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val pagerIndex: Int = 0,
    val lastMediaIndex: Int = 0,
    val showContentPicker: Boolean = false,
    val mediaRefreshProjectId: Long? = null,
    val mediaRefreshToken: Long = 0L
)