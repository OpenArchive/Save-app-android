### Goal
Migrate the app’s local persistence from Sugar ORM to Room with zero data loss, minimal disruption, and a gradual rollout. This document inventories the current Sugar usage, proposes a target architecture, details a phased migration plan, defines the Room schema and data migration path, and breaks work into actionable tasks with risks and validation steps.

---

### Current state (quick audit of SugarORM usage)

- Library initialization
    - `SaveApp` extends `com.orm.SugarApp` (file: `app/src/main/java/net/opendasharchive/openarchive/SaveApp.kt`).
    - AndroidManifest config for Sugar (file: `app/src/main/AndroidManifest.xml`):
        - `<meta-data android:name="DOMAIN_PACKAGE_NAME" android:value="net.opendasharchive.openarchive.db" />`
        - `<meta-data android:name="DATABASE" android:value="openarchive.db" />`
        - `<meta-data android:name="VERSION" android:value="37" />`
        - `<meta-data android:name="QUERY_LOG" android:value="true" />`

- Core entities (each extends `SugarRecord` and is used directly across layers):
    - `Space` (file: `db/Space.kt`)
        - Relations/queries: `getAll()`, `get(type, host, username)`, `get(id)`, `projects`, `archivedProjects`, `hasProject(...)` use Sugar static queries (`find`, `findById`, `first`, `findAll`).
        - Side effects: a write loop in `license` setter updates all related `Project`s then `save()`s each project.
    - `Project` (file: `db/Project.kt`)
        - Relations: `collections`, `openCollection` lazily creates and `save()`s a `Collection` then `save()`s the `Project`.
    - `Collection` (file: `db/Collection.kt`)
        - `media` computed via `find(Media::class.java, "collection_id = ?", ...)`.
        - Custom cascade `delete()` over child `Media`.
    - `Media` (file: `db/Media.kt`)
        - Many primitive fields plus `Date`, `ByteArray`, and `status` enum (backed by `Int`).
        - Queries: `getByStatus(...)`, `get(id)`, and relations: `collection`, `project`, `space` via `findById`.

- Direct CRUD calls scattered in UI and services (non-exhaustive, representative):
    - `.save()` used in: `features/main/adapters/MainMediaAdapter.kt`, `features/settings/FolderDetailFragment.kt`, `upload/UploadService.kt`, `services/webdav/WebDavViewModel.kt`, `features/media/*`, `features/internetarchive/*`, `db/UploadMediaAdapter.kt`, etc.
    - `.delete()` used in: `features/main/adapters/MainMediaAdapter.kt`, `db/Collection.kt`, `db/Project.kt`, `db/Space.kt`, `features/main/ui/MainMediaScreen.kt`, `features/settings/FolderDetailFragment.kt`, `services/webdav/WebDavViewModel.kt`, etc.
    - Example called out by you: `features/main/MainMediaFragment.kt`
        - `refresh()` calls `Collection.getByProject(mProjectId)` then uses `collection.media` (Sugar query) to populate UI and headers.

- Threading
    - Many calls are from UI contexts; Room prohibits main-thread I/O by default. We will need to move all DB I/O behind suspend functions and Dispatchers.IO.

- DI
    - Koin is used. We can add a Room module and swap bindings as we migrate.

- Tables (conceptual)
    - `Space` (server/account), `Project` (folder within a space), `Collection` (a batch/list of items uploaded together), `Media` (individual media items).
    - Relationships based on fields observed:
        - `Project.spaceId -> Space.id`
        - `Collection.projectId -> Project.id`
        - `Media.projectId -> Project.id`, `Media.collectionId -> Collection.id`

---

### Design principles for the migration

- No direct framework entity exposure beyond the data layer.
    - Introduce domain models (POJOs/data classes) not tied to Sugar/Room.
    - Map persistence entities <-> domain models with mappers.
- Hide DB behind repositories + data sources.
    - UI/ViewModels only depend on repository interfaces returning domain models and `Flow`s.
- Gradual, incremental refactor:
    1) Introduce abstractions around existing Sugar,
    2) change feature code to use abstractions,
    3) bring Room side-by-side,
    4) one-time data migration,
    5) flip bindings to Room,
    6) remove Sugar.
- Preserve primary keys across migration to avoid breaking references.
- Use Koin feature flags/bindings to switch implementation without broad code change.

---

### Target architecture (layers and contracts)

- Domain layer (new)
    - Data classes: `SpaceModel`, `ProjectModel`, `CollectionModel`, `MediaModel` (nullability/types aligned to current use, not DB details).
    - Interfaces (repositories):
        - `SpaceRepository`
            - `suspend fun getAll(): List<SpaceModel>`
            - `suspend fun getById(id: Long): SpaceModel?`
            - `suspend fun upsert(space: SpaceModel)`
            - `suspend fun delete(id: Long)`
            - `fun observeCurrent(): Flow<SpaceModel?>` (if needed)
        - `ProjectRepository`
            - `suspend fun getById(id: Long): ProjectModel?`
            - `fun observeBySpace(spaceId: Long): Flow<List<ProjectModel>>`
            - `suspend fun upsert(project: ProjectModel)`
            - `suspend fun delete(id: Long)`
        - `CollectionRepository`
            - `fun observeByProject(projectId: Long): Flow<List<CollectionModel>>`
            - `suspend fun getById(id: Long): CollectionModel?`
            - `suspend fun upsert(collection: CollectionModel)`
            - `suspend fun delete(id: Long)`
        - `MediaRepository`
            - `fun observeByCollection(collectionId: Long): Flow<List<MediaModel>>`
            - `suspend fun getByStatus(statuses: List<MediaStatus>, order: MediaOrder? = null): List<MediaModel>`
            - `suspend fun upsert(media: MediaModel)`
            - `suspend fun bulkUpsert(items: List<MediaModel>)`
            - `suspend fun delete(id: Long)`
    - Mapping: extension functions `Media.toModel()`, `MediaEntity.fromModel()`, etc.

- Data layer (new contracts, two implementations)
    - Local data sources (interfaces) backing the repositories.
    - Implementation A (first): `SugarLocalDataSource` (wraps existing Sugar models and APIs). Only this touches `.save()`, `.delete()`, `find*`.
    - Implementation B (later): `RoomLocalDataSource` using Room DAOs.

- DI
    - Provide repository interfaces bound to either Sugar or Room data sources with a single Koin switch.

- Concurrency
    - All repository methods are `suspend`/`Flow` with Dispatchers.IO context.

---

### Room schema design

- Entities (sketch; keep field names to match existing DB columns where possible to ease data copy):

    - `@Entity(tableName = "SPACE", indices = [Index("type"), Index("username"), Index("host")])`
        - `@PrimaryKey(autoGenerate = false) val id: Long`
        - `val type: Int`
        - `val name: String`
        - `val username: String`
        - `val displayname: String`
        - `val password: String`
        - `val host: String`
        - `val metaData: String`
        - `val licenseUrl: String?`

    - `@Entity(tableName = "PROJECT", indices = [Index("spaceId"), Index("archived")], foreignKeys = [FK to SPACE(id) ON DELETE CASCADE])`
        - `@PrimaryKey(autoGenerate = false) val id: Long`
        - `val description: String?`
        - `val created: Date?`
        - `val spaceId: Long`
        - `val archived: Boolean`
        - `val openCollectionId: Long` (if needed, otherwise derive)
        - `val licenseUrl: String?`

    - `@Entity(tableName = "COLLECTION", indices = [Index("projectId")], foreignKeys = [FK to PROJECT(id) ON DELETE CASCADE])`
        - `@PrimaryKey(autoGenerate = false) val id: Long`
        - `val projectId: Long`
        - `val uploadDate: Date?`
        - `val serverUrl: String?`

    - `@Entity(tableName = "MEDIA", indices = [Index("projectId"), Index("collectionId"), Index("status"), Index("priority"), Index("createDate")],
            foreignKeys = [FK to PROJECT(id) ON DELETE CASCADE, FK to COLLECTION(id) ON DELETE CASCADE])`
        - `@PrimaryKey(autoGenerate = false) val id: Long`
        - `val originalFilePath: String`
        - `val mimeType: String`
        - `val createDate: Date?`
        - `val updateDate: Date?`
        - `val uploadDate: Date?`
        - `val serverUrl: String`
        - `val title: String`
        - `val description: String`
        - `val author: String`
        - `val location: String`
        - `val tags: String`
        - `val licenseUrl: String?`
        - `val mediaHash: ByteArray` (or store as base64 string)
        - `val mediaHashString: String`
        - `val status: Int`
        - `val statusMessage: String`
        - `val projectId: Long`
        - `val collectionId: Long`
        - `val contentLength: Long`
        - `val progress: Long`
        - `val flag: Boolean`
        - `val priority: Int`
        - `val selected: Boolean` (if necessary for persistence; may be better as UI-only)

- Type converters
    - `Date <-> Long (epoch)`
    - `Uri` currently is not stored as Uri in DB (stored as path `String`), so no converter needed.

- DAOs
    - `SpaceDao`, `ProjectDao`, `CollectionDao`, `MediaDao` with query methods mirroring the repository needs.
    - Provide `@Transaction` queries for relationships if you need `SpaceWithProjects`, `ProjectWithCollections`, `CollectionWithMedia` for efficient loading.

- Database
    - `@Database(entities = [...], version = 1, exportSchema = true)` initially independent of Sugar’s versioning.
    - Consider naming the Room DB file differently (e.g., `openarchive_room.db`) to isolate from Sugar’s `openarchive.db` and simplify data copy.

Notes:
- Preserve `id` values (no `autoGenerate`) to maintain referential integrity and avoid remapping.
- Add missing constraints (FKs) and indices for performance and consistency (Sugar does not enforce FKs by default).

---

### Data migration strategy (Sugar → Room)

We will run both Sugar and Room side-by-side during migration to safely copy data, then switch reads/writes to Room.

- Approach
    - Keep Sugar DB as-is (`openarchive.db`). Create a new Room DB file (e.g., `openarchive_room.db`).
    - On first startup after shipping Room, enqueue a one-time WorkManager job to copy data entity-by-entity from Sugar to Room.
    - During copy, pause critical background writers to prevent race conditions (e.g., `UploadService`).
    - Copy order: Space → Project → Collection → Media (respecting FK ordering). Each copy in a Room transaction batch.
    - ID preservation: Insert into Room using the existing `id` as primary key. Disable/avoid autogeneration.

- Detection and idempotency
    - Maintain a `DataMigrationState` table in Room with `sourceVersion` (37), `completedAt`, and entity-level progress markers to allow resume.
    - Before starting, check if Room already contains any rows; if yes and `DataMigrationState` says completed, skip. If partial, resume.

- Transformation rules
    - Dates: copy as-is; ensure nullability maps correctly.
    - Enums: `Media.status` is `Int`, copy directly.
    - Binary: `mediaHash` `ByteArray` copy as BLOB.
    - UI-only fields (e.g., `selected`) can be reconsidered; if used across sessions, keep; otherwise drop from Room and keep transient in domain/UI.

- Concurrency/race control
    - Temporarily pause or quiesce write flows during migration:
        - Gate repository write methods with a “migration in progress” flag.
        - Stop/pause `UploadService` and any sync jobs (e.g., via a foreground notice and delayed start) until migration completes.

- Verification
    - After copy, validate record counts per table match (Sugar count vs. Room count).
    - Spot-check referential integrity (every `Project.spaceId` exists in `Space`, etc.).
    - Optional: hash totals (e.g., sum of `contentLength` and count of `status` values) to ensure data parity.

- Rollback
    - If migration fails, leave app bound to Sugar implementation and show a non-blocking error + retry option.

- Clean-up (post cutover)
    - After a stable release proves the migration, remove Sugar dependencies and, optionally, delete `openarchive.db` on a later app version (with user consent/backup).

---

### Phased migration plan (soft, incremental)

Phase 0 — Prep (no behavior change)
- Add new domain models and repository interfaces (no wiring yet).
- Inventory and document all direct Sugar usages that will be touched (we have the hotspots list and can expand as needed).

Phase 1 — Introduce Sugar-backed data sources and repositories
- Implement `SugarLocalDataSource` for each entity using current Sugar queries.
- Implement `*RepositoryImpl` that call `SugarLocalDataSource` and expose `suspend`/`Flow` APIs.
- Koin DI: bind repos to Sugar implementations.
- Move UI, ViewModels, services to use repositories instead of calling `.save()/.delete()/find*()` directly.
    - Example: `MainMediaFragment.refresh()` currently calls `Collection.getByProject(...)` and `collection.media`. Replace with `collectionRepository.observeByProject(projectId)` and `mediaRepository.observeByCollection(...)` combined into a UI model.
    - Example: `MainMediaAdapter` writes (`item.save()`, `item.delete()`): replace with `mediaRepository.upsert(...)`/`delete(...)`.
- Ensure all repository calls are on Dispatchers.IO (Room-ready standard).
- Keep entities extending `SugarRecord` intact; mappers convert to and from domain models at repo boundaries.

Phase 2 — Add Room side-by-side (read/write still Sugar through repos)
- Add Room `@Entity`s, `@Dao`s, `@Database` and type converters (no app binding yet).
- Add a Room Koin module providing the DB and DAOs.
- Implement `RoomLocalDataSource` mirroring the Sugar data source contract.
- Implement data migration worker (reads from Sugar repos, writes to Room DAOs). Do not run yet.

Phase 3 — One-time data copy and dual-write (optional short window)
- Ship a release that:
    - Runs the migration worker on first start, copying Sugar → Room.
    - Option A (safer, simpler): keep all live reads/writes still going through Sugar until migration completes; repositories are unaware.
    - Option B (if data churn is heavy): temporarily enable dual-write in repo implementations (write to Sugar then Room) for a short period. This increases complexity—prefer Option A if possible by pausing writers during migration.

Phase 4 — Flip repositories to Room
- Toggle DI bindings to point repositories to `RoomLocalDataSource` (feature flag or build-time toggle).
- Validate app behavior end-to-end with Room.

Phase 5 — Remove Sugar
- Change `SaveApp` base class from `SugarApp` to `Application` (or keep until a cleanup release). Remove manifest Sugar meta-data if no longer used.
- Remove Sugar dependencies, entity base class inheritance, and remaining legacy code.
- On a subsequent release, optionally delete the old Sugar DB file after a grace period (with backup/confirmation).

---

### Detailed mapping of current hotspots to be refactored to repositories

- UI Fragments/Activities (examples)
    - `features/main/MainMediaFragment.kt`
        - Replace `Collection.getByProject(projectId)`/`collection.media` with repository streams.
        - Replace any implicit state changes (`.delete()` cascades) with explicit repository calls.
    - `features/main/MainActivity.kt` (multiple `.save()` calls, selected project ops)
    - `features/settings/FolderDetailFragment.kt` (`mProject.save()`, `mProject.delete()`)
    - `features/media/*` (`PreviewActivity`, `ReviewActivity`, `Picker`, etc. call `.save()`/`.delete()`).
    - `features/main/ui/MainMediaScreen.kt` (Compose screens calling `.save()`/`.delete()`).

- Adapters/ViewHolders
    - `features/main/adapters/MainMediaAdapter.kt` (`item.save()`, `item.delete()`)
    - `db/UploadMediaAdapter.kt` (similar patterns)

- Services/ViewModels
    - `upload/UploadService.kt` (frequent `media.save()` status/progress writes)
    - `services/webdav/WebDavViewModel.kt` and `services/webdav/WebDavFragment.kt` (`space.save()`, `.delete()`)
    - `features/internetarchive/...` (`space.save()`)

- Entities with hidden side effects to untangle
    - `Project.openCollection`: creates/saves a `Collection` and saves the `Project`.
        - Move this logic into a Repository method (e.g., `ProjectRepository.getOrCreateOpenCollection(projectId)`).
    - `Space.license` setter updates child projects and saves them.
        - Move to a use case/interactor in the domain layer, not an entity property setter.
    - `Collection.delete()` cascade over `Media`.
        - Rely on Room FK ON DELETE CASCADE, or a repository-level `@Transaction` method.

---

### Threading and reactive data

- Replace any synchronous Sugar calls in UI with repository `Flow`s and `suspend` functions, e.g.:
    - `Flow<List<ProjectModel>>` by space
    - `Flow<List<CollectionWithHeader>>` by project (header values computed once you have the list of `MediaModel`)
    - `Flow<List<MediaModel>>` by collection, ordered by status and id (as Sugar does now: `"status, id DESC"`)
- Scope these flows to ViewModels and collect on the UI with lifecycle awareness.

---

### Migration mechanics (step-by-step)

1) Ship abstractions (Phase 1)
- Define domain models.
- Define repository interfaces and data source contracts.
- Implement `SugarLocalDataSource` + mappers.
- Adopt repositories across the identified hotspots.

2) Introduce Room (Phase 2)
- Create Room schema and DAOs.
- Wire Room into DI but keep repos bound to Sugar.

3) Implement data copy job (Phase 3)
- WorkManager `OneTimeWorkRequest` with constraints (battery, storage, not low memory).
- Steps inside the worker (all on Dispatchers.IO):
    - Check `DataMigrationState` in Room; if not started:
        - Read all Spaces from Sugar (via Sugar data source) in batches; insert into Room in a transaction.
        - For each table, verify counts; persist progress to `DataMigrationState` to support resume.
        - Repeat for Projects, Collections, Media.
    - Mark complete and write `completedAt`.
- Pause `UploadService` while copying Media (most write-heavy) and resume afterward.

4) Flip to Room (Phase 4)
- Toggle Koin to bind repositories to `RoomLocalDataSource`.
- Monitor crash/error reports; keep Sugar present for a release as a safety net.

5) Cleanup (Phase 5)
- Remove Sugar-specific code: entity inheritance from `SugarRecord`, `SaveApp : SugarApp`, manifest metadata.
- Optional: provide a DB backup/export option and then delete `openarchive.db`.

---

### Risks and mitigations

- Writes during migration (race conditions)
    - Mitigate by pausing upload/services; or gate writes via a migration flag in repositories; or dual-write briefly (higher complexity).

- ID collisions / referential integrity
    - Preserve IDs, copy parent tables first, add FKs in Room so issues surface early during copy.

- Main-thread I/O regressions
    - Repository APIs enforce `suspend`/`Flow` on Dispatchers.IO.

- Hidden side effects in entity setters/derived properties
    - Move to use cases and repositories; cover with tests.

- Schema drift
    - Align Room fields and types with current Sugar tables; where unknown, log and inspect at runtime via `sqlite_master` read before copy.

- Large media list performance
    - Add indices on `status`, `priority`, `createDate`, and FKs; use paging if needed.

---

### Validation and QA plan

- Unit tests
    - Repositories against in-memory Room DB.
    - Mappers round-trip tests between domain and entities.

- Migration tests
    - Instrumentation test that seeds a Sugar DB snapshot (from a real device or fixture), runs the migration worker, then validates counts and key invariants.

- Feature tests
    - Smoke tests for: listing spaces/projects/collections/media, create/edit/delete, upload flows, and license propagation.

- Observability
    - Add structured logs for migration progress `[MIGRATION]` and failure metrics for quick triage.

---

### Task breakdown (actionable)

Epic A: Abstractions and Sugar wrapping
- A1. Define domain models (`SpaceModel`, `ProjectModel`, `CollectionModel`, `MediaModel`) and `MediaStatus` enum.
- A2. Define repository interfaces for Space/Project/Collection/Media.
- A3. Implement `SugarLocalDataSource` for each entity; implement mapping.
- A4. Implement repository implementations delegating to Sugar data sources.
- A5. Wire Koin to provide repositories (Sugar-backed).
- A6. Replace direct Sugar calls in hotspots:
    - A6.1 `MainMediaFragment` (refresh, delete flows)
    - A6.2 `MainMediaAdapter` and `UploadMediaAdapter`
    - A6.3 `UploadService` (status updates → `MediaRepository`)
    - A6.4 `FolderDetailFragment`, `MainActivity` (project ops)
    - A6.5 WebDAV and InternetArchive flows (`SpaceRepository` usage)
- A7. Move side-effecting entity logic into repositories/use-cases (e.g., `Project.openCollection`, `Space.license`).

Epic B: Room introduction
- B1. Define Room entities, converters, DAOs, and DB class; create Koin module.
- B2. Implement `RoomLocalDataSource` for each entity.
- B3. Build repository integration tests using Room in-memory DB.

Epic C: Data migration
- C1. Implement `DataMigrationState` table in Room.
- C2. Implement WorkManager worker for Sugar → Room copy with batch/transaction handling.
- C3. Add app-level coordination to pause/resume `UploadService` during migration.
- C4. Add migration progress notification (user-friendly, cancellable).
- C5. Add post-migration validation and logging.

Epic D: Cutover
- D1. Feature-flag or config to flip DI bindings from Sugar to Room.
- D2. Release to internal testers / beta channel; monitor.
- D3. Rollout to production.

Epic E: Decommission Sugar
- E1. Switch `SaveApp` to extend `Application` (remove SugarApp).
- E2. Remove Sugar dependencies, entity inheritance, and manifest metadata.
- E3. Optional: offer DB cleanup (delete old `openarchive.db` after backup/export) in a later release.

---

### Notes on your initial idea (Local DataSource abstraction)

- You’re on the right track. The key is to:
    - Define repository interfaces at the domain boundary used by UI/ViewModels.
    - Implement a Sugar-backed local data source first so you can stop calling `.save()`/`.delete()` directly anywhere.
    - Later, swap the implementation to a Room-backed local data source without touching UI code. This yields a smooth migration.
- Also introduce domain models now, and map from Sugar entities to domain. This makes future Room adoption trivial.

---

### Specific examples for immediate refactor (showing how code shape changes)

- `MainMediaFragment.refresh()` today:
    - Reads `Collection.getByProject(projectId)` and uses `collection.media`.
- After Phase 1:
    - `collectionRepository.observeByProject(projectId)` returns `Flow<List<CollectionModel>>`.
    - For each collection, `mediaRepository.observeByCollection(collection.id)` feeds the adapter. Or provide a combined `CollectionWithMedia` projection from Room for efficiency once flipped.

- `MainMediaAdapter` deletion today:
    - Calls `item.delete()` and sometimes `collection?.delete()`.
- After Phase 1:
    - Calls `mediaRepository.delete(mediaId)`; `Collection` cleanup becomes an explicit repository operation (or FK cascade in Room later).

- `UploadService` writes:
    - Replace `media.save()` status updates with `mediaRepository.upsert(updatedModel)`; repository manages batching to reduce I/O.

---

### Rollout strategy

- 1 release with abstractions (no user-visible changes).
- 1 release introducing Room and running the one-time migration in the background but keeping Sugar active for live operations.
- 1 release flipping to Room for all operations; Sugar still included as fallback.
- Final release removing Sugar entirely.

---

### Acceptance criteria checklist

- All direct calls to `SugarRecord` static methods and `.save()/.delete()` are removed from UI/VM/services and replaced with repository calls.
- Room entities/DAOs created with indices and FKs; tests pass for basic CRUD and relationship queries.
- Migration worker copies data 1:1; counts match; verified on a realistic dataset.
- Upload, listing, and editing flows function identically on Room.
- No main-thread DB access crashes; all DB ops on IO dispatcher.
- Sugar fully removed by the final phase; `SaveApp` no longer extends `SugarApp`.

---

### Appendix: Code references (as observed)

- Entities
    - `app/src/main/java/net/opendasharchive/openarchive/db/Space.kt` (Sugar queries in `companion object`, `projects`, `archivedProjects`; `.save()` in `license` setter; `.delete()` cascade)
    - `app/src/main/java/net/opendasharchive/openarchive/db/Project.kt` (`openCollection` performs `.save()` twice; `.delete()` cascade)
    - `app/src/main/java/net/opendasharchive/openarchive/db/Collection.kt` (`media` property uses `find(...)`; custom `.delete()` cascade)
    - `app/src/main/java/net/opendasharchive/openarchive/db/Media.kt` (`getByStatus`, relations to `Collection`, `Project`, `Space`)

- App init and manifest
    - `app/src/main/java/net/opendasharchive/openarchive/SaveApp.kt` extends `SugarApp`
    - `app/src/main/AndroidManifest.xml` Sugar `meta-data` (DB name, version)

- Hotspots invoking `.save()`/`.delete()` (representative)
    - `features/main/adapters/MainMediaAdapter.kt`
    - `db/UploadMediaAdapter.kt`
    - `features/main/MainActivity.kt`
    - `features/settings/FolderDetailFragment.kt`
    - `upload/UploadService.kt`
    - `features/media/*` (`PreviewActivity`, `ReviewActivity`, `Picker`)
    - `services/webdav/WebDavFragment.kt`, `services/webdav/WebDavViewModel.kt`
    - `features/internetarchive/*`
    - `features/main/ui/MainMediaScreen.kt` (Compose)