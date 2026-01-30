package net.opendasharchive.openarchive.features.main.ui

import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.media.AddMediaType

sealed class HomeAction {
    data object Load : HomeAction()
    data object Reload : HomeAction() // Force reload projects
    data class SelectSpace(val spaceId: Long) : HomeAction()
    data class SelectProject(val projectId: Long?) : HomeAction()
    data class UpdatePager(val page: Int) : HomeAction()
    data object AddClick : HomeAction()
    data object AddLongClick : HomeAction()
    data class TabSelected(val tab: HomeBottomTab) : HomeAction()
    data object ContentPickerDismissed : HomeAction()
    data class ContentPickerPicked(val type: AddMediaType) : HomeAction()
    data object ShowUploadManager : HomeAction()
    data object HideUploadManager : HomeAction()
    data class Navigate(val route: AppRoute) : HomeAction()
    data object NavigateToAddNewFolder : HomeAction()
    data object NavigateToArchivedFolders : HomeAction()
    data object NavigateToPreviewMedia : HomeAction()
    data object NavigateToCamera: HomeAction()
    data class MediaImported(val projectId: Long) : HomeAction()

    // NEW: Project-level actions that modify the projects list
    data class RenameProject(val projectId: Long, val newName: String) : HomeAction()
    data class ArchiveProject(val projectId: Long) : HomeAction()
    data class DeleteProject(val projectId: Long) : HomeAction()
}