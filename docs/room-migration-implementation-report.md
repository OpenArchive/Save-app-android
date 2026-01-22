# Room Migration Support - Implementation Report

This document summarizes the technical changes implemented to support the transition from SugarORM to Room persistence, as outlined in [room-migration.md](room-migration.md).

## Completed Architectural Changes

The primary focus has been on **Phase 1: Abstraction**, which ensures that the business logic and UI are decoupled from the specific persistence framework.

### 1. Introduction of Domain Models

We have introduced stable domain models in the `net.opendasharchive.openarchive.core.domain` package. These are pure Kotlin data classes that represent the app's core entities without any dependency on SugarORM or Room.

| Entity | Domain Model | Purpose |
| :--- | :--- | :--- |
| **Media** | [Evidence.kt](../app/src/main/java/net/opendasharchive/openarchive/core/domain/Evidence.kt) | Represents a media asset with metadata and [EvidenceStatus](../app/src/main/java/net/opendasharchive/openarchive/core/domain/Evidence.kt). |
| **Collection** | [Submission.kt](../app/src/main/java/net/opendasharchive/openarchive/core/domain/Submission.kt) | A group of Evidence items (formerly known as Collection). |
| **Project** | [Archive.kt](../app/src/main/java/net/opendasharchive/openarchive/core/domain/Archive.kt) | Represents a folder or project (formerly known as Project). |
| **Space** | [Vault.kt](../app/src/main/java/net/opendasharchive/openarchive/core/domain/Vault.kt) | Represents a sync target or backend (formerly known as Space). |

### 2. Repository Pattern Implementation

The persistence logic has been abstracted behind repository interfaces. These interfaces expose reactive `Flow`s and `suspend` functions, ensuring that all database I/O is performed off the main thread.

*   **[SpaceRepository.kt](../app/src/main/java/net/opendasharchive/openarchive/core/repositories/SpaceRepository.kt)**: Manages Space/Vault entities and the current selected space.
*   **[ProjectRepository.kt](../app/src/main/java/net/opendasharchive/openarchive/core/repositories/ProjectRepository.kt)**: Handles Project/Archive creation, renaming, and state.
*   **[MediaRepository.kt](../app/src/main/java/net/opendasharchive/openarchive/core/repositories/MediaRepository.kt)**: Handles CRUD for individual media items (Evidence).
*   **[WebDavRepository.kt](../app/src/main/java/net/opendasharchive/openarchive/services/webdav/WebDavRepository.kt)**: Specialized repository for WebDAV space interactions.
*   **[CollectionRepository.kt](../app/src/main/java/net/opendasharchive/openarchive/core/repositories/CollectionRepository.kt)**: Manages Submissions.

### 3. Abstraction of SugarORM

Existing SugarORM entities (e.g., `db/Media.kt`, `db/Project.kt`) are now "wrapped" by their respective repository implementations.
*   Calculated properties and side effects (like cascade deletes or auto-creating collections) have been moved into the repository implementations.
*   Mappers are used to convert Sugar database models to Domain Models at the repository boundary.

## Benefits for Room Migration

*   **Zero Leakage**: ViewModels and UI no longer call `.save()` or `.delete()` on Sugar entities. All interactions happen via Repository interfaces.
*   **Reactive UI**: The use of `Flow` means the UI automatically reacts to local database changes once they are triggered in the repository.
*   **Atomic Swap**: When the Room implementation is ready, the `Sugar*Repository` implementation can be replaced with a `Room*Repository` in the DI module (Koin) with **zero changes** to the feature/UI code.
