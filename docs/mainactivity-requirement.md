You are a senior Android/Compose architect.  
Your task is to **design a new, Compose-first implementation** of the S.A.V.E. / OpenArchive **Main screen** that preserves the existing user-facing behavior and UX, but **does not mirror the legacy XML/Fragment implementation**. You are free to propose better, cleaner internal architecture as long as the **visuals and interactions match the current app (or improve smoothness/performance)**.

Cross-check everything below against the codebase and correct any mismatches you find.

---

## 1. Scope (Files & Features to Analyze)

### 1.1 Main screen & navigation

Focus your analysis and migration plan on the following legacy components:

- `app/src/main/java/net/opendasharchive/openarchive/features/main/MainActivity.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ProjectAdapter.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/MainMediaFragment.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/adapters/MainMediaAdapter.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/adapters/MainMediaViewHolder.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/SectionViewHolder.kt`

Plus upload-related plumbing:

- `app/src/main/java/net/opendasharchive/openarchive/upload/BroadcastManager.kt`
- `app/src/main/java/net/opendasharchive/openarchive/upload/UploadService.kt`

These represent the current XML/Fragment-based implementation of:

- MainActivity shell and its navigation
- ViewPager2 and project tabs
- MainMediaFragment and its adapters
- FolderBar
- Custom bottom bar + FAB
- Right-side navigation drawer
- Upload status plumbing (BroadcastManager + UploadService → UI)

### 1.2 Data model & Sugar ORM

The current persistence layer uses **Sugar ORM** with at least the following entities:

- `app/src/main/java/net/opendasharchive/openarchive/db/Space.kt`
- `app/src/main/java/net/opendasharchive/openarchive/db/Project.kt`
- `app/src/main/java/net/opendasharchive/openarchive/db/Collection.kt`
- `app/src/main/java/net/opendasharchive/openarchive/db/Media.kt`

Relationships (verify in code):

- One `Space` has many `Project`s.
- Each `Project` has many `Media` items, grouped into `Collection`s.
- There is a global notion of a **current selected space** `Space.current`.
- At the Activity level, if at least one space exists, there must always be a selected space.

Important **Sugar ORM caveat**:

- When using Kotlin `data class .copy()` on Sugar ORM-backed entities, the generated `id` field (managed by Sugar ORM) **is not preserved** on the copy.
- If `.save()` is called on a copied instance without the original `id`, Sugar can persist it as a **new record** instead of updating the existing one.
- The new **repository layer MUST be designed defensively** to:
    - Avoid accidental inserts when an update is intended.
    - Provide safe APIs that do not expose raw Sugar entities directly to the UI layer.
    - Either:
        - Work with explicit IDs and update methods, or
        - Use separate domain models and map to/from Sugar entities carefully.

### 1.3 Out of scope / Already handled

The following are **not part of this migration** and should be treated as plug-in components:

- **Settings screen**:
    - Assume there is (or will be) a Compose-based settings screen that can be used as the last page in the pager.
    - You do **not** need to plan a migration of the legacy `SettingsFragment` itself.
- **Media grid list item internals**:
    - The UI for a single media card (thumbnail, overlays, status badges, etc.) is already (or will be) handled as a separate Compose component.
    - For this plan, treat it as a reusable `MediaGridItem` composable with the same behavior; you do not need to refactor its internals.

Your primary focus is to **reimagine the Main screen in Compose**:

- MainActivity-level shell
- FolderBar
- ViewPager / pager behavior
- MainMedia tab behavior
- Custom bottom bar + FAB
- Right-side drawer
- Upload status integration
- Proper state management for spaces/projects/media and upload progress

The new implementation should be **architecturally clean**, even if it means a full rewrite of these parts.

---

## 2. Functional Requirements (Behavioral Summary)

This section describes the **required behavior** of the new Compose Main screen, independent of how the legacy XML implementation currently achieves it.

### 2.1 Overall Main screen behavior

In the current app, the Main screen:

1. Shows a **FolderBar** inside the Media content (for project/selection controls) when on a Media page.
2. Displays a **pager** with:
    - One page per project in the currently selected `Space` (each showing that project’s media).
    - A final page for the **Settings** screen.
3. Has a **custom bottom navigation bar** with:
    - Tab: **Media**
    - Tab: **Settings**
    - Center **+** FAB (add button).
4. Exposes a **right-side navigation drawer** with:
    - Space switcher.
    - Project list for the current space.
    - “+ New Folder” action.

The bottom bar, pager, FolderBar, drawer, and upload system must all work together so that:

- Spaces and projects can be switched smoothly.
- Media is shown for the selected project.
- Upload progress is visible and reactive for the **currently selected project**.
- The UX feels **identical to or smoother than** the current implementation.

You are **explicitly NOT required** to match the internal architecture of the XML implementation. Instead, design the **best Compose + coroutines/Flow architecture** that satisfies these behaviors.

### 2.2 Navigation flows & secondary screens

Beyond the main Home/Media/Settings layout, the app must support navigation to several secondary screens.

- From the **Settings** page, users can navigate to:
    - ProofMode settings screen (e.g., `ProofModeSettingsScreen`).
    - Space list screen (e.g., `SpaceListScreen`).
    - Folder list screen (e.g., `FolderListScreen`).
    - Passcode setup screen (e.g., `PasscodeSetupScreen`).

- From the **drawer**:
    - The “+ New Folder” button navigates to an **Add Folder** flow (e.g., `AddFolderScreen`).
    - In the expandable space list, the **last item** is an “Add server” row that navigates to a **Space setup** flow (e.g., `SpaceSetupScreen`).

- From the **bottom bar + FAB**:
    - If there is **no space**, tapping/long-pressing the add button should navigate to `SpaceSetupScreen` instead of directly opening import pickers.
    - If there is a space but **no project** in the current space, tapping/long-pressing the add button should navigate to `AddFolderScreen`.
    - If there is a selected project and media list is shown, tapping media with status `New` or `Local` opens `PreviewMediaList` for that project.

---

## 3. FolderBar Behavior (Top Bar)

The **FolderBar** has three modes:

1. **Default mode**
2. **Selection mode**
3. **Edit mode**

Visibility:

- Only visible when on a **Media/project page** (not on the Settings page).
- Conceptually part of the **MediaListScreen body**, positioned below the global top app bar; it is *not* the app-wide top bar itself.

### 3.1 Default mode

Scenarios:

1. **No space available**
    - FolderBar is effectively empty (shows nothing).
2. **Selected space has no projects**
    - Shows only the **space icon** (no project info).
3. **Selected space has at least one project**
    - Shows:
        - Space icon
        - Selected project name
        - Total media count in the project
        - An “Edit” popup/menu with actions (below).

### 3.2 Edit popup actions

From Default mode, the Edit menu offers:

1. **Rename folder**
    - Switches FolderBar to **Edit mode**.
    - Edit mode shows:
        - Close button.
        - Text field pre-filled with the selected project name.
    - User can rename and save the project.
2. **Select media**
    - Switches FolderBar to **Selection mode**.
    - Selection mode UI shows:
        - Close button.
        - “Select media —— Remove” label/button with icon.
3. **Archive folder**
    - Archives the current project.
4. **Remove folder**
    - Shows a confirmation dialog via `DialogManager`.
    - On confirmation, removes the project.

Effects of **archive/remove**:

- If successful, the project is removed from the **current space’s project list**.
- Results:
    - If another project remains in the space:
        - It becomes the **newly selected project**.
        - Its media is shown in the pager.
    - If no projects remain:
        - The Media page shows the **“no projects” empty state** (see Section 4.2).

### 3.3 Selection mode

Selection mode is entered when:

- The user **long-presses a media item** in the Media screen, or
- The user selects **“Select media”** from the FolderBar Edit menu.

Behavior:

- FolderBar shows:
    - Close button.
    - Label text like “Select media”.
    - A “Remove” action/button with an icon.
- The Media list:
    - Long-press selects the first item & enters Selection mode.
    - Tapping other items toggles their selection.
- When **no items are selected anymore**, FolderBar must automatically revert to **Default mode**.

---

## 4. Pager, Tabs, and Empty States (Media vs Settings)

### 4.1 Pager and selected project

Pager behavior:

- One **Media** page per project in the current space.
- One **Settings** page as the last page.
- Example:
    - 4 projects ⇒ 4 Media pages + 1 Settings page = 5 pages.

Selected project logic:

- When a **Media page** is visible, the selected project is the one backing that page.
- When the **Settings page** is visible:
    - There is still a **“last active” project index** used for:
        - FAB add/import behavior.
        - Upload-related UI if needed.

The bottom navigation bar and pager must stay **fully synchronized**:

- Swiping the pager updates the active bottom-nav tab.
- Choosing tabs in bottom-nav updates the pager position.

### 4.2 Empty states in the Media screen

For each Media page, three empty states exist:

1. **No space**
    - Show a welcome view:
        - Message.
        - “Tap to add server” CTA.
        - Arrow pointing down.
2. **No projects in the selected space**
    - Show:
        - “Tap to add folder” CTA.
        - Arrow down.
3. **No media in the selected project**
    - Show:
        - “Tap to add media” CTA.
        - Arrow down.

The “selected project” is always the one backing the current Media page (or the “last active” one if the Settings page is visible).

---

## 5. Right-Side Navigation Drawer

### 5.1 Availability

- Drawer is **disabled** when there is **no space**.
- Drawer is **enabled** when at least one space exists.
- The **hamburger icon**:
    - Only visible when the **Media tab** is active.
    - Hidden when the **Settings tab** is active.

### 5.2 Drawer layout & content

From top to bottom:

1. **Servers label** with dropdown arrow:
    - Tapping toggles an **expandable space list** (all spaces).
    - `Space.current` (the currently selected space) is highlighted.
    - The **last item** in this list is an “Add server” row that navigates to the Space setup flow (`SpaceSetupScreen`).
    - The expanded panel visually **slides over** the current space + project list area instead of pushing it down.
2. Divider.
3. **Current selected space** (icon + name).
4. **Project list** for the current space:
    - Rows:
        - Folder icon + project name.
    - The currently selected project is highlighted.
5. Bottom button: **“+ New Folder”**:
    - Tapping:
        - Closes the drawer.
        - Navigates to the “Add Folder” screen.

### 5.3 Drawer interactions → Main content

- Tapping a **project**:
    - Closes the drawer.
    - Pager scrolls to the Media page for that project.
- Tapping a **space** in the space list:
    - Updates the **current selected space**.
    - Pager reloads pages:
        - One Media page per project in the new space.
        - Last page is Settings.
    - Drawer closes.
    - Next time the drawer opens, the project list reflects the new space’s projects.
    - For the new space, the initially selected project is typically the first one (confirm via code).
- Tapping the **“Add server”** item in the space list:
    - Closes the drawer.
    - Navigates to `SpaceSetupScreen` where the user can configure a new space/server.

---

## 6. Media Screen: Sections & Click Behavior

Within each Media page (for a project):

- Media is grouped into **collections** (often by date).
- Each collection:
    - Has a **section header**:
        - Shows date/timestamp and number of items in that section.
    - Shows a **3-column media grid** below.
- Adapters from the legacy code:
    - `MainMediaAdapter`
    - `MainMediaViewHolder`
    - `SectionViewHolder`
    - These will be replaced by a Compose list (e.g., `LazyColumn` containing `SectionHeader` + `LazyVerticalGrid`).

**Media item behavior** (high-level, independent of rendering):

- `media.status == New` or `Local`:
    - Navigate to `PreviewMediaList` screen.
- `media.status == Queued`:
    - Open the **Upload Manager bottom sheet** (hosted at the Main screen level).

Selection rules (reiterated):

- Long press selects the item and enters Selection mode.
- Tap toggles selection.
- When selection becomes empty, FolderBar returns to Default mode.

---

## 7. Add Button (Bottom FAB) & Import Flows

The center **+** button in the bottom bar supports:

1. **onClick**:
    - Opens gallery picker via `Picker.import`.
    - Imported media:
        - Copied to app cache directory.
        - Persisted via Sugar ORM.
2. **onLongClick**:
    - Opens a **content picker bottom sheet** with options:
        - Camera
        - Gallery
        - File manager
    - All these flows use `Picker.import` internally.

In addition, the add button behavior must respect the current empty state:

- If there is **no space**, the add button (tap or long press) should take the user to `SpaceSetupScreen` rather than opening import pickers.
- If there is a selected space but **no project**, the add button (tap or long press) should take the user to `AddFolderScreen`.
- Only when there is a selected project in the current space should the add button open the import flows (gallery/camera/file manager) using `Picker.import`.

### 7.1 Shared media import (Android intents)

- MainActivity can receive **shared media from other apps**.
- Shared files are imported using `Picker.import`:
    - Copied into app cache.
    - Persisted into DB.

### 7.2 Interaction with the Settings page

- If the pager is currently on the **Settings** page and the user taps or long-presses the **+** button:
    - The app must:
        1. Switch back to the **previously selected project’s Media page**.
        2. Execute the relevant add/import action as if triggered directly from that project’s Media page.

---

## 8. Upload Pipeline & Upload Manager

### 8.1 UploadService (JobService)

- `UploadService` (a `JobService`) is responsible for:
    - Finding all `Media` with `status = Queued` across **all spaces and projects**.
    - Uploading them.
    - Updating their status and progress in the DB.

This service is **global**, not scoped to a single project.

### 8.2 BroadcastManager (current pattern)

- `BroadcastManager` is used to send real-time upload events to the UI:
    - Status transitions:
        - `Queued → Uploading`
        - `Uploading → Uploaded`
        - `Uploading → Errored`
        - Deletion events.
    - Progress updates as percentage.

Current flow:

1. `UploadService` updates the DB and sends a broadcast via `BroadcastManager`.
2. `MainMediaFragment` receives these broadcasts and updates the UI:
    - Show upload start.
    - Show progress.
    - Show success/error.
    - Show deletions.

Important constraint:

- Even though uploads span multiple spaces/projects, the Main screen UI should only reflect upload state for the **currently selected project** (pager’s current project page).

### 8.3 Upload Manager bottom sheet

- The **Upload Manager bottom sheet** is hosted at the Main screen level (currently `MainActivity`).
- Behavior:
    - When the bottom sheet is **opened**, the upload service is **paused**.
    - Inside the sheet, the user can:
        - Reorder queued media.
        - Delete queued media.
    - After the sheet is **dismissed**:
        - Upload service **resumes**.
        - The Media screen must reflect the updated state (order, deletions, statuses).

---

## 9. Target Compose Architecture (High-Level Direction)

This section describes the **direction** the new implementation should take. You should validate and refine it based on the actual codebase and best practices.

### 9.1 High-level Compose structure

We expect something along the lines of:

- `HomeScreen` composable hosted by `MainActivity`, containing:
    - A **Scaffold** (or equivalent layout) with:
        - A **global top app bar** that:
            - Shows the app logo and a **hamburger icon** to open the right-side drawer when on the Home/Media graph.
            - Shows a **back arrow** and a text title (plus optional actions) when on secondary screens (e.g., settings sub-screens, space/folder lists, passcode setup, etc.).
        - A **custom bottom nav bar + center FAB** that is visible only for the Home/Media/Settings destinations (hidden on secondary screens).
        - A right-side **drawer** (e.g., `ModalNavigationDrawer` or a custom drawer) that hosts the Servers/Spaces/Projects UI described above.
        - A content area that can host a **NavHost** for the main navigation graph, within which the Home destination contains the project **HorizontalPager** (or equivalent) and the per-project `MediaListScreen` (which itself shows the FolderBar in its content).
- Pager content:
    - For each project in the current space:
        - A `MediaListScreen(projectId = ...)`.
    - As the last page:
        - A plug-in **SettingsScreen** composable (already migrated or to be supplied separately).

> Note: It is acceptable to use a single top-level Scaffold around a NavHost, or to have per-screen Scaffolds for certain destinations, as long as the behavior above is preserved. The recommended default is a single top-level Scaffold that conditionally shows the Home-specific app bar and bottom bar for the Home graph only.

You are not required to use this exact structure, but your final architecture must:

- Be idiomatic Compose.
- Avoid fragment-based ViewPager2.
- Keep pager, bottom bar, FolderBar, and drawer behavior consistent with the existing UX.

### 9.2 State ownership and ViewModels

Proposed state responsibilities (to be validated/optimized):

- **HomeViewModel** (activity-scoped via Koin):
    - Single source of truth for:
        - `spaceList`
        - `currentSpace`
        - `projectsInCurrentSpace`
        - `selectedProject` (for the current space)
        - Pager index + “last active project” when on Settings.
        - Drawer expansion state, etc. (or delegate some UI-only state to Compose).
    - Exposes state as `StateFlow`/`Flow` suitable for Compose.

- **MediaListViewModel(projectId: Long)** (screen-scoped, possibly keyed by `projectId`):
    - Responsible for:
        - Loading media collections and items for that project.
        - Handling media-level actions (click, long-press selection, delete, etc.).
        - Reacting to upload events related to its `projectId`.
    - Must **not** duplicate space/project selection state already owned by `HomeViewModel`.

Key requirement:

- There must be a **clear single source of truth** for:
    - Current space
    - Projects of that space
    - Selected project
- Media-specific state should be local to `MediaListViewModel`, but selection mode state must be coordinated with FolderBar and Home screen.

You should propose the **best state structure** to avoid duplication and race conditions, using the above as a starting point, not a rigid spec.

---

## 10. Repository Layer & Room-Ready Design

We want to **extract Sugar ORM usage into repositories** now, in a way that is:

- Safe despite the `.copy()` / `id` issue.
- Ready to be backed by **Room + coroutines/Flow** in the future.

You must:

1. Read and respect `docs/room-migration.md` and align your repository design with the strategy described there.
2. Propose interfaces such as (names are suggestions; adjust as needed):

    - `SpaceRepository`
        - `suspend fun getSpaces(): List<SpaceDomain>`
        - `suspend fun getCurrentSpace(): SpaceDomain?`
        - `suspend fun setCurrentSpace(spaceId: Long)`
    - `ProjectRepository`
        - `suspend fun getProjects(spaceId: Long): List<ProjectDomain>`
        - `suspend fun renameProject(projectId: Long, newName: String)`
        - `suspend fun archiveProject(projectId: Long)`
        - `suspend fun deleteProject(projectId: Long)`
    - `MediaRepository`
        - `fun observeMediaCollections(projectId: Long): Flow<List<MediaCollectionDomain>>`
        - `suspend fun upsertMedia(...)`
        - `suspend fun updateMediaStatus(mediaId: Long, status: MediaStatus)`
        - Etc.

3. Decide how to:
    - Map between Sugar entities (`Space`, `Project`, `Media`, `Collection`) and domain models.
    - Handle ID preservation correctly (avoid accidental new rows).
    - Expose **Flow-based** APIs suitable for Compose, even if they are backed by broadcast listeners or manual refresh for now.

The repository layer should **fully encapsulate** Sugar ORM; UI and ViewModels must not call `.save()`/`.delete()` on Sugar entities directly.

---

## 11. Upload & Progress Handling (Modern Replacement for BroadcastManager)

You must design a **Compose/coroutine-friendly mechanism** that replaces (or wraps) `BroadcastManager`:

- The new design should:
    - Expose upload state as `Flow<UploadEvent>` or similar.
    - Be safely consumed from Compose ViewModels and screens.
    - Filter events by `projectId` so that each `MediaListScreen` only handles relevant updates.
    - Provide a way for the Upload Manager bottom sheet and `UploadService` to:
        - Pause/resume uploads.
        - Reorder queued items.
        - Delete items.
    - Keep the implementation **compatible with the existing `UploadService`** during the transition.

Possible approaches (you decide and justify):

- A central `UploadRepository` or `UploadManager` singleton that:
    - Listens to broadcasts from `UploadService`.
    - Translates them into strongly-typed events.
    - Exposes them as Flows.
- Or refactor `UploadService` to send events via a coroutine channel / shared Flow instead of raw broadcasts, if feasible without a huge refactor.

Key constraint:

- We only need to surface **upload-related events for the currently selected project** into the visible Media page and FolderBar (e.g., to show “uploading”, progress bars, etc.).

---

## 12. Migration Strategy (Full Rewrite of Main Screen)

We want a **clean, full migration** of the Main screen, not a long, incremental hybrid.

Your migration plan should:

- Treat the new Compose main screen as a **greenfield implementation** (within the same module) that:
    - Uses the new repository layer.
    - Uses new ViewModels and state management.
    - Does **not** reuse the existing XML/Fragment logic or adapter classes directly.
- Describe how to:
    - Introduce the repository layer and ViewModels.
    - Implement the new `HomeScreen` and Media pager in Compose.
    - Integrate the existing Compose **SettingsScreen** and media item components.
    - Replace the legacy MainActivity entry point with the new Compose-based main screen.
- Include concrete steps such as:
    - Phase 1: Introduce repositories and domain models, still referenced from legacy code as needed.
    - Phase 2: Implement new Compose `HomeScreen` + pager + drawer + FolderBar + Upload Manager using the repositories.
    - Phase 3: Wire MainActivity to host only the new Compose UI and remove legacy fragments/adapters.
    - Phase 4: Clean-up and deprecate any unused legacy Sugar-based logic that bypasses the repositories.

Although the plan can mention “phases”, the **goal state** is a full replacement of the old MainActivity+ViewPager+MainMediaFragment+adapters with the new Compose implementation, not a partial or mixed solution.

---

## 13. Deliverables You Must Produce

When you analyze the code and this requirements document, produce:

1. **Requirements Summary**
    - A concise, structured recap of the **intended behavior and UX** of the Main screen (derived from both the document and the actual code, resolving any discrepancies).
2. **Compose Migration Architecture**
    - Proposed composable tree:
        - `HomeScreen`, FolderBar, drawer, bottom bar, pager, `MediaListScreen`, `SettingsScreen`, Upload Manager bottom sheet.
    - ViewModel structure and state ownership:
        - Activity vs screen scope, flows, and single sources of truth.
    - How pager, bottom bar, drawer, FolderBar, and upload flows are wired together.
3. **Repository Layer Design**
    - Interfaces and responsibilities for Space/Project/Media/Upload repositories.
    - How they encapsulate Sugar ORM and avoid the `.copy()` id issue.
    - How they align with the Room migration strategy in `docs/room-migration.md`.
4. **Upload & Progress Handling Design**
    - A modern replacement (or wrapper) for `BroadcastManager` that works well with Compose.
    - Filtering of upload events per project.
    - Interaction model between UploadService, Upload Manager bottom sheet, and Media screens.
5. **Full Migration Plan**
    - A clear, step-by-step plan to move from the legacy main screen to the new Compose implementation.
    - Explicit notes on any temporary compatibility layers or transitional code, with a clear path to fully remove legacy fragments/adapters.

Use this document as the **functional source of truth**, but always reconcile it against the actual code in the listed files and the Room migration document. If you find any inconsistencies, adjust your analysis and proposed plan to match the **real behavior the app relies on**, not assumptions from the legacy architecture.