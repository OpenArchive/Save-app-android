# HomeScreen Architecture Improvements

## Current Issues Analysis

### 1. State Management Problems

**Issue:** HomeViewModel and MainMediaViewModel have overlapping responsibilities
- HomeViewModel manages projects list but MainMediaViewModel also loads project data
- When MainMediaViewModel renames/archives/deletes a folder, HomeViewModel doesn't know
- `homeState.copy(selectedProjectId = projectId)` creates unnecessary state copies (HomeScreen.kt:205)

**Issue:** No reactive connection between ViewModels
- Changes in MainMediaViewModel (rename, archive, delete) don't propagate to HomeViewModel
- Requires manual refresh or app restart to see changes

**Issue:** Project info redundantly loaded
- HomeViewModel loads projects via ProjectRepository
- MainMediaViewModel loads same project again via ProjectRepository (MainMediaViewModel.kt:152)

### 2. Navigation Event Confusion

**Issue:** Mixed navigation responsibilities
- HomeViewModel emits `HomeEvent.Navigate` for app-level navigation
- MainMediaViewModel emits `MainMediaEvent.NavigateToPreview` for screen navigation
- Both ViewModels have navigation events but unclear ownership

### 3. Pager State Synchronization

**Issue:** Pager doesn't react to project list changes
- When a project is deleted, pager pages don't update until navigation occurs
- `totalPages` calculated in HomeScreenContent but not reactive to HomeViewModel changes

## Proposed Improvements

### Architecture Principles

1. **HomeViewModel = Single Source of Truth** for:
   - All spaces
   - Current selected space
   - All projects in current space
   - Currently selected project ID
   - Pager index

2. **MainMediaViewModel = Media-Scoped State** for:
   - Collections and media for ONE project
   - Selection mode and selected media IDs
   - FolderBar mode (INFO/SELECTION/EDIT)

3. **Upward Event Propagation**:
   - MainMediaViewModel emits actions that affect project state
   - HomeViewModel handles them and updates its state
   - Re-composition propagates changes down

### Key Changes

#### Change 1: Add Project-Level Actions to HomeViewModel

```kotlin
// New actions in HomeAction
data class RenameProject(val projectId: Long, val newName: String) : HomeAction()
data class ArchiveProject(val projectId: Long) : HomeAction()
data class DeleteProject(val projectId: Long) : HomeAction()
```

#### Change 2: MainMediaViewModel Emits Project Actions Upward

```kotlin
// In MainMediaViewModel, replace direct repository calls with events:
sealed class MainMediaEvent {
    data class RequestProjectRename(val projectId: Long, val newName: String) : MainMediaEvent()
    data class RequestProjectArchive(val projectId: Long) : MainMediaEvent()
    data class RequestProjectDelete(val projectId: Long) : MainMediaEvent()
    // ... existing events
}
```

#### Change 3: HomeScreen Bridges Events

```kotlin
// HomeScreen bridges MainMediaViewModel events → HomeViewModel actions
LaunchedEffect(Unit) {
    mediaViewModel.uiEvent.collectLatest { event ->
        when (event) {
            is MainMediaEvent.RequestProjectRename ->
                homeViewModel.onAction(HomeAction.RenameProject(event.projectId, event.newName))
            is MainMediaEvent.RequestProjectArchive ->
                homeViewModel.onAction(HomeAction.ArchiveProject(event.projectId))
            // ...
        }
    }
}
```

#### Change 4: Remove Project Loading from MainMediaViewModel

MainMediaViewModel should NOT load project info. It only needs projectId.

#### Change 5: HomeViewModel Exposes Project Details

```kotlin
// In HomeViewModel
fun getProject(projectId: Long): Project? =
    _uiState.value.projects.find { it.id == projectId }
```

## Implementation Details

See the improved implementations in the following files.
