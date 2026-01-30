# Room Migration Analysis (Sugar ORM -> Room)

This report analyzes the current Sugar ORM models and repositories for Space/Project/Collection/Media and provides a concrete migration guide for a Room-based implementation, including a one-time data migration strategy, risks, and mitigations. It builds on `docs/room-migration.md` and `docs/room-migration-implementation-report.md`.

## Scope and context

- Existing domain model and repository abstractions are in place (Phase 1), mapping Sugar entities to domain models:
  - Space -> Vault
  - Project -> Archive
  - Collection -> Submission
  - Media -> Evidence
- The current persistence backend remains Sugar ORM via `Sugar*Repository` implementations.
- Goal: Introduce Room alongside Sugar, migrate data once, then switch repositories to Room without touching UI/business logic.

## Current Sugar ORM models (data + behavior)

### Space (`db/sugar/Space.kt`)
- Fields: `type`, `name`, `username`, `displayname`, `password`, `host`, `metaData`, `licenseUrl`.
- Static queries:
  - `getAll()`
  - `get(type, host?, username?)`
  - `get(id)`
- Relationships:
  - `projects` and `archivedProjects` via `Project` query on `space_id` and `archived` flag.
- Side effects:
  - `license` setter updates **all related Projects** and saves them.
  - `delete()` cascades through child Projects.
- UI helpers (avatars) and derived values (`friendlyName`, `initial`, `hostUrl`).

### Project (`db/sugar/Project.kt`)
- Fields: `description`, `created`, `spaceId`, `archived`, `openCollectionId`, `licenseUrl`.
- Queries:
  - `getById(id)`.
- Relationships:
  - `collections` by `project_id`.
  - `space` derived via `spaceId`.
- Side effects:
  - `openCollection` lazily creates and saves a `Collection` if missing or closed.
  - `delete()` cascades through child Collections.
  - `isArchived` setter pulls `Space.license` when unarchiving.

### Collection (`db/sugar/Collection.kt`)
- Fields: `projectId`, `uploadDate`, `serverUrl`.
- Queries:
  - `getByProject`, `getByProjectRecentFirst`, `get(id)`.
- Relationships:
  - `media` (ordered `status, id DESC`).
- Side effects:
  - `delete()` cascades through child Media.

### Media (`db/sugar/Media.kt`)
- Fields: heavy metadata set; notable types:
  - `Date` fields (create/update/upload)
  - `ByteArray` for `mediaHash`
  - `status` enum stored as Int
  - `selected` boolean (UI-ish but persisted)
- Queries:
  - `getByStatus(statuses, order)`
  - `get(id)`
- Relationships:
  - `collection`, `project`, `space` via lazy `findById`.
- Behaviors:
  - computed properties (`isUploading`, `tagSet`, `fileUri`, etc).

## Current Sugar repositories (behavioral contracts)

The `Sugar*Repository` classes are now the single integration point with Sugar ORM. Key behaviors to preserve in Room:

- **SpaceRepository**
  - `observeSpaces()` and `observeCurrentSpace()` are driven by `InvalidationBus` signals.
  - `setCurrentSpace(id)` updates `Prefs.currentSpaceId` and invalidates dependent flows.

- **ProjectRepository**
  - `getProjects(vaultId)` returns non-archived projects (`Space.projects` behavior).
  - `getActiveSubmission(projectId)` uses `Project.openCollection` (creates a collection if missing).
  - `archiveProject` must apply license when unarchiving (via `Project.isArchived` setter behavior).

- **CollectionRepository**
  - `getCollections` uses most-recent-first ordering (`id DESC`).

- **MediaRepository**
  - `getMediaForCollection` orders by `status, id DESC`.
  - `deleteMedia` deletes collection when size would fall below 2.
  - `queueAllForUpload`, `retryMedia`, `updatePriority`, `setSelected` mutate status/flags.

Room equivalents must explicitly implement these behaviors (there is no implicit entity logic).

## Recommended Room data model (Kotlin-first, Room features)

Create Room entities under a data layer package (e.g. `core/db/room/`), independent of domain models. Preserve IDs and relationships exactly to keep migration straightforward. Prefer Kotlinx datetime in the domain and map to `Long` epoch values at the Room boundary.

### Entities (suggested fields)

- `RoomVaultEntity` (SPACE)
  - `@PrimaryKey val id: Long`
  - `val type: Int`
  - `val name: String`
  - `val username: String`
  - `val displayname: String`
  - `val password: String`
  - `val host: String`
  - `val metaData: String`
  - `val licenseUrl: String?`
  - Indexes: `type`, `username`, `host`

- `RoomArchiveEntity` (PROJECT)
  - `@PrimaryKey val id: Long`
  - `val description: String?`
  - `val created: Long?` (epoch ms, from kotlinx.datetime)
  - `val spaceId: Long`
  - `val archived: Boolean`
  - `val openCollectionId: Long`
  - `val licenseUrl: String?`
  - Indexes: `spaceId`, `archived`
  - FK to SPACE(id) ON DELETE CASCADE

- `RoomSubmissionEntity` (COLLECTION)
  - `@PrimaryKey val id: Long`
  - `val projectId: Long`
  - `val uploadDate: Long?` (epoch ms, from kotlinx.datetime)
  - `val serverUrl: String?`
  - Index: `projectId`
  - FK to PROJECT(id) ON DELETE CASCADE

- `RoomEvidenceEntity` (MEDIA)
  - `@PrimaryKey val id: Long`
  - `val originalFilePath: String`
  - `val mimeType: String`
  - `val createDate: Long?` (epoch ms, from kotlinx.datetime)
  - `val updateDate: Long?` (epoch ms, from kotlinx.datetime)
  - `val uploadDate: Long?` (epoch ms, from kotlinx.datetime)
  - `val serverUrl: String`
  - `val title: String`
  - `val description: String`
  - `val author: String`
  - `val location: String`
  - `val tags: String` (semi-colon joined for parity)
  - `val licenseUrl: String?`
  - `val mediaHash: ByteArray`
  - `val mediaHashString: String`
  - `val status: Int`
  - `val statusMessage: String`
  - `val projectId: Long`
  - `val collectionId: Long`
  - `val contentLength: Long`
  - `val progress: Long`
  - `val flag: Boolean`
  - `val priority: Int`
  - Indexes: `projectId`, `collectionId`, `status`, `priority`, `createDate`
  - FKs to PROJECT/COLLECTION ON DELETE CASCADE

### Type converters
- Use `Long` epoch values for Kotlinx `LocalDateTime` in domain and Room entities.
- Keep `ByteArray` as BLOB.
- Use Kotlinx `Duration` in domain where needed and store as `Long` millis if persisted in the future.

### Mapping
- Reuse domain mappers but add Room equivalents (Room entity <-> domain) to avoid leaking Room into domain/UI.
- Keep `tags` as a `String` in Room and convert to `List<String>` in the domain layer (as today).
- Do not persist `isSelected`; treat it as UI-only in the domain layer.

## Room DAO and repository behavior mapping

Room repositories should preserve the same behavior as the Sugar repositories but replace `InvalidationBus` with reactive Room queries:

### DAO sketches
- `VaultDao`
  - `@Query("SELECT * FROM SPACE") fun observeSpaces(): Flow<List<RoomVaultEntity>>`
  - `@Query("SELECT * FROM SPACE WHERE id = :id") suspend fun getById(id: Long): RoomVaultEntity?`
  - `@Insert(onConflict = REPLACE) suspend fun upsert(entity: RoomVaultEntity): Long`
  - `@Delete suspend fun delete(entity: RoomVaultEntity)`

- `ArchiveDao`
  - `@Query("SELECT * FROM PROJECT WHERE spaceId = :spaceId AND archived = 0 ORDER BY id DESC") fun observeBySpace(spaceId: Long): Flow<List<RoomArchiveEntity>>`
  - `@Query("SELECT * FROM PROJECT WHERE id = :id") suspend fun getById(id: Long): RoomArchiveEntity?`
  - `@Query("SELECT * FROM PROJECT WHERE spaceId = :spaceId AND description = :name LIMIT 1") suspend fun getByName(spaceId: Long, name: String): RoomArchiveEntity?`
  - `@Update suspend fun update(entity: RoomArchiveEntity)`
  - `@Insert(onConflict = REPLACE) suspend fun upsert(entity: RoomArchiveEntity): Long`

- `SubmissionDao`
  - `@Query("SELECT * FROM COLLECTION WHERE projectId = :projectId ORDER BY id DESC") fun observeByProject(projectId: Long): Flow<List<RoomSubmissionEntity>>`
  - `@Query("SELECT * FROM COLLECTION WHERE id = :id") suspend fun getById(id: Long): RoomSubmissionEntity?`
  - `@Insert(onConflict = REPLACE) suspend fun upsert(entity: RoomSubmissionEntity): Long`
  - `@Delete suspend fun delete(entity: RoomSubmissionEntity)`

- `EvidenceDao`
  - `@Query("SELECT * FROM MEDIA WHERE collectionId = :collectionId ORDER BY status, id DESC") fun observeByCollection(collectionId: Long): Flow<List<RoomEvidenceEntity>>`
  - `@Query("SELECT * FROM MEDIA WHERE projectId = :projectId ORDER BY id DESC") fun observeByProject(projectId: Long): Flow<List<RoomEvidenceEntity>>`
  - `@Query("SELECT * FROM MEDIA WHERE status IN (:statuses) ORDER BY createDate DESC") suspend fun getByStatus(statuses: List<Int>): List<RoomEvidenceEntity>`
  - `@Update suspend fun update(entity: RoomEvidenceEntity)`
  - `@Insert(onConflict = REPLACE) suspend fun upsert(entity: RoomEvidenceEntity): Long`
  - `@Query("DELETE FROM MEDIA WHERE id = :id") suspend fun deleteById(id: Long)`

### Room repository considerations

- **Current space**: The app currently stores the selected space in `Prefs` (`Space.current` in Sugar). Moving this to the repository layer is the right direction. For reactivity, expose a `StateFlow<Long?>` that mirrors prefs changes and map it to a Room query for the selected vault. This keeps Home screen reactive without turning selection into a DB row.
- **Open collection creation**: Implement `getActiveSubmission(projectId)` as a `@Transaction` method in the repository or DAO:
  - Read `openCollectionId` from `Project`.
  - If no collection or it has `uploadDate != null`, create a new `Collection`, update `openCollectionId`, return it.
- **License propagation**: On unarchive, if project license is blank, pull from parent space. This logic lives in the repository (not entities).
- **Delete cascade**: Room FKs with ON DELETE CASCADE replace manual delete loops. Keep the special case: delete a Collection when its last Media is removed.
- **Foreign key enforcement**: Use `@ForeignKey(onDelete = CASCADE, onUpdate = CASCADE)` and ensure FK constraints are enabled when the DB opens.

## One-time migration plan (Sugar -> Room)

### Strategy
- Keep Sugar DB (`openarchive.db`) and introduce a new Room DB file (e.g. `openarchive_room.db`) to simplify copy and rollback.
- Execute a one-time migration worker on app start (WorkManager) with progress and retry support.
- Insert in parent-first order to satisfy FKs: Space -> Project -> Collection -> Media.
- Preserve **existing IDs** in Room (`@PrimaryKey(autoGenerate = false)`).

### Idempotency + progress
- Create a Room-only `MigrationState` table containing:
  - `version`, `startedAt`, `completedAt`, `currentStage`, row counts.
- Worker checks:
  - If `completedAt != null` and Room has data, exit.
  - If partial, resume from `currentStage`.

### Data copy flow
1. Begin migration; persist `currentStage = SPACES`.
2. Copy Spaces (Sugar -> Room), verify count, save progress.
3. Copy Projects, verify count, save progress.
4. Copy Collections, verify count, save progress.
5. Copy Media in batches, verify counts and optionally per-status counts.
6. Mark `completedAt` and release migration lock.

### Concurrency control
- Pause `UploadService` until migration completes to avoid Media write races.
- Gate repository writes via a `MigrationGate` flag so UI actions do not mutate data mid-copy.

### Validation checks
- Table row counts match between Sugar and Room per entity.
- Spot-check referential integrity (no orphaned Media, etc.).
- Optional: aggregate checksum (sum of `contentLength` and status counts) to detect drift.

## Risks and considerations

- **Race conditions during migration**: Upload queues and background services can mutate Media while copying.
  - Mitigate with a migration gate + paused upload service.
- **Hidden side effects from Sugar entities**: `Space.license` and `Project.openCollection` currently mutate related records.
  - Must be re-implemented in Room repository transactions.
- **Schema drift / unknown Sugar columns**: Sugar tables may include implicit fields (e.g., Sugar internal columns).
  - Use explicit copy via entities/mappers rather than raw table copy.
- **Date/time conversions**: Domain uses Kotlinx datetime, Sugar uses `Date`, Room stores epoch `Long`.
  - Validate converters; ensure timezone consistency.
- **Performance**: Media tables can be large; indexes are required to keep list screens responsive.
  - Batch inserts and `@Transaction` methods for heavy operations.
- **Current space state**: stored in `Prefs`. Ensure Room repo continues to honor it.
- **UI selection state**: `isSelected` is no longer persisted. Ensure selection state is handled in UI state and survives configuration changes where needed.

## Recommended migration phases (reconciled with current status)

1. **Phase 1 done**: Sugar repositories and domain models already introduced.
2. **Phase 2 (Room side-by-side)**:
   - Add Room entities, DAOs, and database module.
   - Implement Room mappers and `Room*Repository` implementations.
3. **Phase 3 (Migration worker)**:
   - Implement `MigrationState` and the WorkManager job.
   - Add migration gate to pause uploads.
4. **Phase 4 (Switch repos)**:
   - Koin bindings flip from Sugar to Room after migration completion.
5. **Phase 5 (Cleanup)**:
   - Remove Sugar dependencies and metadata after stable release.

## Suggested deliverables checklist

- Room entities + DAOs
- Room database + type converters
- Room repository implementations for Space/Project/Collection/Media
- Migration state table + WorkManager migration job
- Migration gate (pause upload/service writes)
- Validation + logging
- Feature flag or remote config to flip repositories

## Notes on advanced Room usage (recommended)
- Use `@Transaction` for multi-step operations: open submission creation, delete-media-then-collection logic.
- Use `@RewriteQueriesToDropUnusedColumns` on relation queries to reduce cursor overhead.
- Prefer direct DAO queries for list screens; use `@Embedded` + `@Relation` sparingly to avoid large object graphs.
- Consider `PagingSource` for large media lists.
