# Upload Performance Issue Analysis (2026-02-18)

## Summary
During media upload to Internet Archive or WebDAV, UI jank is observed (`Skipped 50+ frames`, `Davey ~850-1045ms`).

This is not caused by one single blocking DB call on the main thread. The dominant issue is **high-frequency progress/event/log traffic** creating excessive main-thread state churn and rendering pressure while upload is active.

## Scope
Investigated code paths:
- `app/src/main/java/net/opendasharchive/openarchive/upload/UploadService.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/Conduit.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/common/network/RequestBodyUtil.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaViewModel.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaScreen.kt`
- `app/src/main/java/net/opendasharchive/openarchive/upload/BroadcastManager.kt`
- `app/src/main/java/net/opendasharchive/openarchive/core/di/DatabaseModule.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/HomeScreen.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppReviewCoordinator.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppUpdateCoordinator.kt`
- `app/src/main/java/net/opendasharchive/openarchive/core/repositories/EvidenceRepositoryImpl.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/webdav/data/WebDavConduit.kt`

## Timeline Correlation From Logs
- `20:21:28` upload starts + review/update service binding + analytics bursts.
- `20:21:28 -> 20:21:30` rapid progress events (`2/100, 4/100, ... 100/100`).
- `20:21:33`, `20:21:43`, `20:21:44`, `20:21:48` repeated skipped frames and long Davey frames.
- This pattern aligns with high event/log/UI update pressure during active upload and UI transition.

## Root Cause Findings

### 1) Progress callback frequency is too high at network layer
- `RequestBodyUtil` reads in **2KB segments** (`SEGMENT_SIZE = 2048`) and flushes each iteration.
- File: `app/src/main/java/net/opendasharchive/openarchive/services/common/network/RequestBodyUtil.kt`
- Why this matters:
  - Tiny chunk size causes very frequent `transferred(...)` callbacks.
  - Per-loop `flush()` increases write overhead.
  - Combined effect amplifies progress event frequency.

### 2) Per-progress event triggers UI state updates on main
- `Conduit.jobProgress(...)` emits progress often (`> last + 1%`), logs each event, posts broadcast, emits flow event.
- File: `app/src/main/java/net/opendasharchive/openarchive/services/Conduit.kt`
- `MainMediaViewModel` collects `UploadEventBus` and updates `transientProgress` for each event.
- File: `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaViewModel.kt`
- That state is part of a `combine(...)` pipeline that remaps sections/media lists, then updates full UI state.
- Result: many main-thread recompositions during active upload.

### 3) Synchronous local broadcast on progress path
- `BroadcastManager.postProgress(...)` uses `sendBroadcastSync(...)`.
- File: `app/src/main/java/net/opendasharchive/openarchive/upload/BroadcastManager.kt`
- This adds synchronous dispatch work for every progress tick.
- In current Compose architecture, the primary reactive path is already `UploadEventBus`; broadcast path appears mostly legacy overhead.

### 4) Debug logging volume is very high
- Room query callback logs every SQL query globally.
- File: `app/src/main/java/net/opendasharchive/openarchive/core/di/DatabaseModule.kt`
- WebDAV logs every transfer callback (`Bytes transferred`, `continueUpload`), which can be extremely frequent.
- File: `app/src/main/java/net/opendasharchive/openarchive/services/webdav/data/WebDavConduit.kt`
- This does not by itself prove main-thread blocking, but contributes CPU/logcat overhead and noise while diagnosing.

### 5) DB access is mostly off-main, but there is avoidable query churn
- Upload orchestration runs on `Dispatchers.IO` in `UploadService`.
- Repository methods generally use `withContext(io)`.
- So this is **not a classic “DB on main” issue**.
- However:
  - `updateEvidence()` performs `getById` then `upsert` (extra query each update).
  - `mapToDomain()` does `archiveDao.getById(...)` per evidence (N+1 pattern).

### 6) In-app review/update checks overlap upload interaction window
- `HomeScreen` always mounts both checks.
- Files:
  - `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/HomeScreen.kt`
  - `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppReviewCoordinator.kt`
  - `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppUpdateCoordinator.kt`
- Log shows Play service binding/errors around same period as upload + navigation.
- This increases contention during sensitive UI moments.

## Non-Root-Cause Clarifications
- Upload pipeline itself is not primarily running on main; orchestration is IO-based.
- The jank is mostly from **event density + synchronous signaling + main-thread state/render churn**.
- SQL logs in this capture mostly represent debug instrumentation, not definitive proof of blocking DB on main.

## Impact
- Perceived lag during upload and navigation.
- Visible jank in media grid while progress updates stream in.
- Harder diagnostics due to excessive log noise.

## Risk If Unchanged
- Poor UX on mid/low-end devices during upload.
- Increased battery/CPU overhead.
- Future regressions become harder to isolate due to noisy telemetry/logging.

## Recommended Direction (high-level)
1. Reduce progress callback/event frequency at source.
2. Remove synchronous legacy broadcast from hot progress path.
3. Reduce main-thread state churn for progress rendering.
4. Gate heavy debug logging.
5. Defer review/update checks when upload/import is active.

For implementation details, see:
- `docs/plans/2026-02-18-upload-performance-remediation-plan.md`
