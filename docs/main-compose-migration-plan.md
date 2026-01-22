# Compose Main Screen Migration Plan

## Requirements Summary (reconciled with legacy code)
- Pager: ViewPager2 hosts one page per project in `Space.current` plus a trailing Settings page (`ProjectAdapter.settingsIndex = max(1, projects.size)` guarantees Settings exists even with 0 projects). Settings currently uses `SettingsFragment` (XML) not Compose.
- Bottom bar + FAB: custom view with tabs (Media, Settings) and center add. When on Settings, add actions jump back to last media page (`mSelectedMediaPageIndex`) before executing. Long-press opens picker sheet when supported. If no space → `SpaceSetupActivity`, if no project → add folder flow. Tap/long-press on Media page opens gallery or picker; hint dialog can gate first import.
- FolderBar: shown only on media pages. Modes: INFO (space + project name + count + edit menu), SELECTION (close + remove), EDIT (rename with IME action). Edit menu actions: rename, select media (toggles selection), archive (toggle flag, saves, refreshes), remove (dialog then delete).
- Selection: long-press in grid enters selection; tap toggles. FolderBar auto exits when none selected. Delete removes media and possibly collection; uploads use `BroadcastManager` to refresh UI. Selection canceled on fragment pause and when page changes if multi-project selection disabled.
- Empty states: welcome CTA varies by no space / no projects / no media (`addMediaHint` shown when no collections).
- Drawer: right-side DrawerLayout, locked on Settings. Visible when there is a space or Dweb group. Space header toggles expandable list; overlays drawer content instead of pushing. Project list highlights current project; selecting navigates pager. “+ New Folder” button closes drawer then launches folder creation. Spaces include “Add account” row (and optional Dweb). Drawer hidden if no space.
- Upload Manager: bottom sheet (Compose or XML) opened from upload status click; pauses `UploadService` on show, resumes if pending uploads on dismiss. Upload events delivered via `BroadcastManager` (`Change` with progress/uploaded flag, `Delete`).
- UploadService: JobService scanning queued/uploading media globally, posts progress via BroadcastManager, stops/starts by MainActivity. Uses Sugar ORM directly.
- Data layer: Sugar entities (`Space`, `Project`, `Collection`, `Media`) used directly in UI/adapters; `.save()` and `.delete()` calls throughout. `Space.current` stored in prefs. `Project.openCollection` lazily creates collection. Selection uses persisted `Media.selected`.

## Compose-First Architecture
- **Main entry**: `MainActivity` hosts a pure Compose `HomeScreen` with a single top-level `Scaffold`.
  - **App bar**: logo + hamburger when on Home graph (Media/Settings). Back arrow + title on secondary screens (ProofMode, Space list, Folder list, Passcode, Add Folder, Space setup, etc.).
  - **Drawer**: right-side `ModalNavigationDrawer` (or custom) containing space switcher (expandable list + add server + optional Dweb), current space summary, project list, and bottom “+ New Folder”. Drawer locked/hidden when no space. Space switch reloads pager to projects of selected space and selects first project by default.
  - **Bottom bar**: custom composable mirroring legacy layout with center FAB. Visible only on Home graph. Pager and bottom bar share a single state (`HomeViewModel`) to keep indices in sync.
  - **Pager**: `HorizontalPager` with one Media page per project in current space plus a final Settings page (Compose Settings screen plug-in). Maintains `lastActiveMediaIndex` for FAB actions when on Settings.
  - **MediaListScreen(projectId)**: shows FolderBar (inside content) and a `LazyColumn` of sections; each section renders header + `LazyVerticalGrid` 3 columns using reusable `MediaGridItem`.
  - **FolderBar modes**: INFO with edit menu (rename, select media, archive, remove), SELECTION (close + remove enabled only when selection non-empty), EDIT (text field prefilled). Visibility gated by pager index < settingsIndex and having a space.
  - **Empty states**: derived from `HomeViewModel` state: no space → “add server”, space with no projects → “add folder”, project with no media → “add media”.
  - **Add button behavior**: `onClick/onLongClick` route through a single handler: no space → `SpaceSetupScreen`, no project → `AddFolderScreen`, else open picker sheet or default gallery; when on Settings, first switch to last media page then run action.
  - **Upload Manager**: composable bottom sheet hoisted at Home level; pauses uploads on show, resumes on dismiss; reads/writes through UploadRepository and exposes reorder/delete/pause/resume.

## ViewModel and State Ownership
- **HomeViewModel (activity-scoped)**  
  - State: `spaces`, `currentSpace`, `projects`, `selectedProjectIndex`, `pagerIndex`, `lastMediaIndex`, drawer open/expanded flags, `hasProjects`, `hasMediaForSelected`, `folderBarMode`, `pendingAddAction`.  
  - Exposes `Flow`/`StateFlow` for Compose; owns side effects for space/project selection, archive/delete/rename project (delegating to ProjectRepository).  
  - Handles add-button routing decisions and coordinates jumping from Settings to media before executing import.
- **MediaListViewModel(projectId)**  
  - State: `collections` with `media` and per-item selection status; derives section headers (uploading vs date/count).  
  - Actions: toggle selection, delete media, open upload manager, observe upload events filtered by `projectId`, expose selection count to FolderBar.  
  - Fetches via `MediaRepository`/`CollectionRepository`; selection kept UI-only (no persisted `selected` flag) to avoid Sugar `.save()` misuse.
- **Settings graph VM(s)**: unchanged; plug existing Compose settings screen into pager.

Synchronization rules:
- Pager index change updates `selectedProjectIndex`, clears selection when leaving media unless multi-selection across projects becomes a requirement.
- Drawer project tap sets pager index; space tap reloads projects and resets selection to first project.
- FolderBar mode driven by `MediaListViewModel` selection state and edit actions; resets on page switch or cancel.

## Repository Layer Design (Sugar-wrapped, Room-ready)
- Domain models (non-Sugar): `SpaceModel`, `ProjectModel`, `CollectionModel`, `MediaModel`, `MediaStatus`, `UploadItem`. IDs are explicit `Long`.
- Interfaces (aligned with `docs/room-migration.md`):  
  - `SpaceRepository`: `observeSpaces(): Flow<List<SpaceModel>>`, `observeCurrent(): Flow<SpaceModel?>`, `setCurrent(spaceId)`, `createOrUpdate(SpaceModel)`, `delete(spaceId)`.  
  - `ProjectRepository`: `observeBySpace(spaceId): Flow<List<ProjectModel>>`, `get(id)`, `rename(id, name)`, `archive(id)`, `delete(id)`, `create(spaceId, name)`, `setLicense(spaceId, licenseUrl)` (for legacy side effects).  
  - `CollectionRepository`: `observeByProject(projectId): Flow<List<CollectionModelWithMedia>>` (or collections + `MediaRepository` combination), `getOpen(projectId): CollectionModel`, `delete(id, cascadeMedia: Boolean = true)`.  
  - `MediaRepository`: `observeByProject(projectId): Flow<List<MediaModel>>` (group client-side into collections), `observeByCollection(collectionId)`, `upsert(media)`, `upsertAll(list)`, `updateStatus(id, status, progress?, statusMessage?)`, `delete(id)`, `deleteMany(ids)`, `reorder(projectId, orderedIds)`, `importShared(uri, projectId, generateProof: Boolean)`.  
  - `UploadRepository`: `observeQueue(projectId?): Flow<List<UploadItem>>`, `pauseAll()`, `resumeAll()`, `reorder(ids)`, `deleteQueued(ids)`, `emitUploadEvents(): Flow<UploadEvent>` (wrapping broadcast), `refreshQueuedFromDb()`.
- Implementation notes:
  - **SugarLocalDataSource** wraps existing entities; all DB I/O on `Dispatchers.IO`. Avoid `data class copy()`; map to/from domain models explicitly, preserve `id`. No `.save()` leakage to UI.  
  - Mapper handles nullable IDs: updates use explicit ID, creates call `save()` then read back ID, preventing accidental inserts.  
  - Selection state should be UI-only; drop `Media.selected` from domain to avoid persisting transient UI flags.
  - Aligns with Room migration doc: same interfaces usable by future `RoomLocalDataSource`; add FK/ID preservation awareness.

## Upload & Progress Handling (Broadcast replacement)
- Introduce `UploadEventBus` / `UploadRepository`:
  - Bridges current `BroadcastManager` by listening once, mapping intents to `UploadEvent` (`Progress`, `Uploaded`, `Deleted`, `Error`) with `projectId` lookup (via repository). Emits through `SharedFlow`.
  - `UploadService` publishes to the bus instead of LocalBroadcast (or dual-publish during transition).  
  - `MediaListViewModel` collects `UploadEvent` filtered by `projectId` to update collection headers and media progress.  
  - Upload Manager bottom sheet reads queue via `UploadRepository.observeQueue` and issues reorder/delete/pause commands; service observes a `Flow<UploadCommand>` (channel) to apply actions.  
  - Pausing behavior: when sheet opens, `UploadRepository.pauseAll()` signals service; on close, `resumeAll()` restarts JobService if queued items exist.

## Migration Plan (Phased)
1) **Abstraction First (no UI change)**  
   - Add domain models and repository interfaces.  
   - Implement Sugar-backed data sources for Space/Project/Collection/Media/Upload using `Dispatchers.IO`.  
   - Refactor MainActivity/MainMediaFragment/adapters/UploadService to use repositories instead of direct `.save()`/`find*` (keep existing UI).  
   - Move side effects (`Project.openCollection`, `Space.license` propagation, persisted selection) into repositories/use-cases.
2) **Compose Home Implementation**  
   - Build `HomeScreen` scaffold, drawer, bottom bar with FAB, HorizontalPager, FolderBar composables, Upload Manager sheet placeholder.  
   - Create `HomeViewModel` + `MediaListViewModel` using repositories; wire empty states, selection, edit/archive/delete flows, add-button routing, drawer interactions.  
   - Integrate existing Compose Settings screen into pager; keep legacy SettingsFragment only until full swap.  
   - Keep legacy MainActivity around behind feature flag until parity proven.
3) **Switch MainActivity Shell**  
   - Replace MainActivity content with Compose `HomeScreen`; remove ViewPager2/Fragment adapters usage.  
   - Ensure navigation to secondary screens (ProofMode, Space list, Folder list, Passcode, Add Folder, Space setup) via NavHost.  
   - Hook upload manager sheet and UploadRepository bridge; remove BroadcastManager listeners from fragments.
4) **Room Readiness & Data Migration**  
   - Add Room entities/DAOs/DB + Koin module per `docs/room-migration.md`.  
   - Implement `RoomLocalDataSource` versions behind same interfaces.  
   - Implement Sugar→Room migration worker (pauses uploads, copies Space→Project→Collection→Media preserving IDs, validates counts).  
   - Keep repositories bound to Sugar during migration copy.
5) **Flip to Room, Clean Legacy**  
   - Switch DI binding to Room implementations (feature flag/config).  
   - Remove legacy XML/Fragment main screen, ProjectAdapter, MainMediaFragment/adapters once Compose path stable.  
   - Remove BroadcastManager usage; keep UploadEventBus.  
   - Plan follow-up to drop Sugar (`SaveApp : Application`, manifest metadata removal) after Room rollout proves stable.

Risks & mitigations:
- Accidental inserts from `.copy()`/`.save()` → avoided via domain models + explicit ID updates in repositories.
- Upload/selection regression → centralize upload events via Flow and keep selection UI-only.
- Pager/drawer sync → single `HomeViewModel` source of truth for space/project/index; no duplicated state in list items.
