# Migration Plan: SugarORM to Room

This plan outlines the technical strategy for migrating the OpenArchive data layer from SugarORM to a modern, fully reactive Room database.

## 1. Analysis of Current State

### Data Structure
The current SugarORM models ([Space](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/sugar/Space.kt#33-231), [Project](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/sugar/Project.kt#19-91), [Collection](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/sugar/Collection.kt#6-56), [Media](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/sugar/Media.kt#14-152)) are located in `db.sugar`. They use manual [Long](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaViewModel.kt#96-97) IDs for relationships and a centralized `InvalidationBus` to trigger re-queries in repositories.

### Repository Pattern & Side Effects
Presently, `SugarXRepository` implementations bridge the Gap between Sugar database calls and Domain models. We must explicitly port legacy entity behaviors that Sugar handled magically:
- **Project Creation**: [getActiveSubmission](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/core/repositories/ProjectRepository.kt#15-16) must lazily create a new [Collection](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/sugar/Collection.kt#6-56) if the current one is "closed" (has an upload date).
- **License Propagation**: `Space.licenseUrl` updates must cascade to all child projects.
- **Auto-Cleanup**: Deleting the last media in a collection must delete the collection itself.
- **Unarchiving**: Restoring a project should re-apply the parent space's license if missing.

## 2. Proposed Room Architecture

### Entities & Relationships
We will define Room entities that mirror the Sugar structure but add performance and integrity features:
- **Indices**: Add indices on foreign keys (`spaceId`, `projectId`, `collectionId`) for faster lookups.
- **Foreign Keys**: Use Room's `ForeignKey` support with `onDelete = CASCADE` to automate what is currently handled manually in `SugarRecord.delete()`.

### Reactivity
By returning `Flow<List<T>>` directly from DAOs, we can eliminate the `InvalidationBus` and `SharedFlow` pinging. The UI will automatically refresh whenever the underlying data changes, reducing complexity and potential "stale data" bugs.

## 3. Implementation Steps

### Phase 1: Foundation
1. **Add Room Dependencies**: Add `androidx.room:room-runtime`, `room-ktx`, and `room-compiler`.
2. **Define Entities**: Create `RoomVaultEntity`, `RoomArchiveEntity`, `RoomSubmissionEntity`, and `RoomEvidenceEntity`.
   - Use [Long](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaViewModel.kt#96-97) epoch for dates (`kotlinx.datetime`).
   - Preservation of IDs: Use `@PrimaryKey(autoGenerate = false)` to retain Sugar IDs.
   - Foreign Keys: Implement `@ForeignKey(onDelete = CASCADE)` between entities.
3. **Type Converters**: Create `DateTypeConverter` and `EnumTypeConverter`.
4. **DAOs**: Create DAOs with `Flow` support to replace the `InvalidationBus`.

### Phase 2: Migration Infrastructure
1. **MigrationState**: Create a table to track stages (`SPACES`, `PROJECTS`, etc.) and row counts.
2. **WorkManager Service**: Implement a robust background worker for the one-time copy.
3. **Migration Gate**: Implement a flag to pause [UploadService](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/upload/UploadService.kt#37-323) and UI writes during the copy to prevent race conditions.

### Phase 3: Repository Transition
1. **Room Implementation**: Create `RoomXRepository` classes. Port all entity-logic side effects mentioned in Phase 1.
2. **Koin Integration**: Create a `roomModule` and switch `FeaturesModule` to use the new implementations ONLY after migration completes.

## 4. Risks and Considerations

> [!WARNING]
> **Data Integrity**
> SugarORM automatically generates IDs. We must ensure that when copying to Room, we explicitly set the [id](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/core/domain/Evidence.kt#14-121) field to match the legacy ID, or the relationships (`projectId`, etc.) will break.

> [!IMPORTANT]
> **The "First Run" Delay**
> For users with thousands of media items, the one-time migration could take several seconds. We should:
> 1. Run the migration on a background thread.
> 2. Show a one-time "Updating database..." progress overlay to prevent the user from interacting with empty lists during the transition.

> [!CAUTION]
> **Schema Evolution**
> SugarORM handled schema changes somewhat loosely. Room requires strict migrations. We should define a clean "Version 1" schema for Room that includes all current functional fields.

## 5. Next Steps
1. Create the `RoomDatabase` and basic entities.
2. Develop the data migration utility.
3. Replace repositories one-by-one or in a single rollout.
