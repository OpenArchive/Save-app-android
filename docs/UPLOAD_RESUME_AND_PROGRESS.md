# Upload Progress and Resume Functionality

This document explains how the OpenArchive app handles upload progress visualization and resume/retry logic for various backends.

## 1. Upload Progress Architecture

To balance UI responsiveness with data durability, the app uses a **hybrid approach** for tracking upload progress.

### Room Database (State Persistence)
The Room database persists the **lifecycle** of an upload but avoids frequent disk writes for every percentage point.
*   **Trigger**: `UploadService.kt` updates the `Evidence` entity.
*   **Phases**:
    *   **Started**: Status set to `UPLOADING` and `progress` set to 0.
    *   **Finished**: Status set to `UPLOADED` and `progress` set to total content length.
    *   **Error**: Status set to `ERROR` with an error message.
*   **Why**: Disk I/O is expensive. Updating the database for every 1% of a large file would drain battery and cause UI jank (due to frequent Room invalidations).

### UploadEventBus (Real-time Observation)
For smooth, 0-100% progress bar animations, the app uses an in-memory event bus.
*   **Implementation**: `UploadEventBus.kt` uses a Kotlin `SharedFlow` with `replay = 1`.
*   **Flow**:
    1.  `Conduit.jobProgress(bytes)` is called during data transfer.
    2.  `UploadEventBus.emitChanged(...)` sends a real-time event.
    3.  `MainMediaViewModel` collects these events and updates the `uiState`.
    4.  `MainMediaScreen` displays the fluid progress.

---

## 2. Resume Functionality

The app supports resuming interrupted uploads for specific backends.

### WebDAV (e.g., Nextcloud)
WebDAV backends support true **resumable uploads** via a chunking strategy implemented in `WebDavConduit.kt`.

*   **Mechanism**:
    1.  Files > 10MB are split into **2MB chunks**.
    2.  **Existence Check**: Before uploading a chunk, the app queries the server to see if that specific byte-range chunk already exists.
    3.  **Resume**: If an upload is interrupted at 50% and restarted, the app identifies that the first half of the chunks are already on the server, skips them, and resumes from the first missing chunk.
    4.  **Assembly**: Once all chunks are present, a `MOVE` or `MKCOL/PUT` sequence (depending on server implementation) assembles them into the final file.

### Internet Archive
The Internet Archive implementation (`IaConduit.kt`) currently **does not support resuming** in the middle of a file.
*   **Mechanism**: Uses a standard single `PUT` request via OkHttp.
*   **Behavior**: If an upload is interrupted, the next attempt will start from byte 0.

---

## 3. Key Components Reference

| Component | Responsibility |
| :--- | :--- |
| `UploadService.kt` | Manages the background `JobService` and orchestrates the upload queue. |
| `Conduit.kt` | Abstract base class for backend-specific upload logic. |
| `UploadEventBus.kt` | In-memory `SharedFlow` for real-time UI updates. |
| `WebDavConduit.kt` | Implements chunked, resumable WebDAV uploads. |
| `IaConduit.kt` | Implements Internet Archive standard uploads. |
| `RequestBodyUtil.kt` | Utility for creating streaming request bodies with progress tracking. |
