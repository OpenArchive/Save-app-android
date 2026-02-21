# Upload Performance Remediation Plan (Phase-wise)

## Goal
Eliminate visible upload-time jank and reduce frame drops without changing product behavior.

Behavior to preserve:
- Upload starts and progresses as today.
- Upload manager and media grid still show progress.
- Final status transitions remain correct.

## Success Criteria
1. No repeated `Skipped 50+ frames` bursts during normal upload on test device.
2. Progress UI remains responsive and accurate.
3. Upload throughput is same or better.
4. No regression in upload completion/error handling.
5. Log volume reduced significantly in debug during upload sessions.

## Baseline Symptoms (Observed)
- High-frequency progress logs (`2%, 4%, ...`) in short intervals.
- Repeated long frame durations (`Davey` around ~850-1045ms).
- Concurrent in-app review/update service activity during same interaction window.

## Phase 0: Baseline Measurement (before changes)
Collect one reproducible baseline run:
- Scenario A: Upload 1 image (1-5 MB).
- Scenario B: Upload 1 video (20-100 MB).
- Scenario C: Upload batch of 10 mixed files.

Capture:
- Frame stats (`Skipped frames`, `Davey` count).
- Upload duration per item.
- Progress event count per item.
- Total SQL log lines (debug build).

Acceptance for Phase 0:
- We can compare after each phase quantitatively.

## Phase 1: Reduce Hot-Path Progress Frequency (highest impact)

### 1.1 Increase network copy segment size and remove per-iteration flush
File:
- `app/src/main/java/net/opendasharchive/openarchive/services/common/network/RequestBodyUtil.kt`

Changes:
- Increase `SEGMENT_SIZE` from `2048` to `65536`.
- Remove `flush()` inside `while` loop in `writeAll(...)`; rely on sink lifecycle.

Why:
- Fewer callbacks and less I/O overhead.

Risk:
- Very low. This is standard buffered streaming behavior.

### 1.2 Throttle progress emission in `Conduit.jobProgress`
File:
- `app/src/main/java/net/opendasharchive/openarchive/services/Conduit.kt`

Changes:
- Emit progress on one of these conditions:
  - progress advanced by >= 5%, OR
  - elapsed >= 250ms since last emission, OR
  - progress == 100.
- Keep monotonic progress and final 100% guarantee.

Why:
- Main UI does not need 50+ updates per second.

Risk:
- Low. Visual progress may be less granular but smoother.

Validation:
- Ensure progress bar still reaches 100 and status transitions are correct.

## Phase 2: Remove Legacy Sync Broadcast Pressure

### 2.1 Stop sync broadcast for per-progress updates
File:
- `app/src/main/java/net/opendasharchive/openarchive/services/Conduit.kt`
- `app/src/main/java/net/opendasharchive/openarchive/upload/BroadcastManager.kt`

Changes:
- Remove `BroadcastManager.postProgress(...)` from hot path.
- Keep `UploadEventBus.emitChanged(...)` as the canonical reactive mechanism.
- Retain broadcast only for delete/change if still needed by any legacy component.

Why:
- `sendBroadcastSync(...)` in tight loop is expensive and largely redundant now.

Risk:
- Medium if hidden receiver still depends on progress broadcasts.

Validation:
- Verify:
  - Main media grid updates progress.
  - Upload manager updates progress.
  - No legacy fragment screen regresses (if still used).

Fallback option:
- If needed, convert sync broadcast to async and gate behind feature flag.

## Phase 3: Reduce MainMedia recomposition churn

### 3.1 Avoid full list remap for every progress tick
File:
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/MainMediaViewModel.kt`

Current problem:
- `transientProgress` changes trigger `combine(...)` that rebuilds all sections and media mapping.

Target:
- Keep DB-derived sections stable.
- Apply transient progress closer to item rendering or with narrower state updates.

Implementation options:
1. Preferred: separate flow/state for progress map and consume per-item in UI.
2. Alternative: keep map in VM but avoid rebuilding all sections when only one media item changes.

Risk:
- Medium (state pipeline refactor).

Validation:
- Compare recomposition count and frame behavior during upload.

## Phase 4: Logging and Diagnostics Hygiene

### 4.1 Gate Room query callback to debug-only
File:
- `app/src/main/java/net/opendasharchive/openarchive/core/di/DatabaseModule.kt`

Changes:
- Register `.setQueryCallback(...)` only for debug builds or when an explicit diagnostics flag is enabled.

### 4.2 Remove per-byte/per-callback WebDAV info logs
File:
- `app/src/main/java/net/opendasharchive/openarchive/services/webdav/data/WebDavConduit.kt`

Changes:
- Keep only coarse logs (start, completion, failure).
- Remove repetitive callback logs from `transferred(...)` and `continueUpload()`.

Why:
- Cleaner signal, lower runtime overhead.

## Phase 5: Reduce Competing Work During Upload Window

### 5.1 Defer review/update checks while import/upload active
Files:
- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/HomeScreen.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppReviewCoordinator.kt`
- `app/src/main/java/net/opendasharchive/openarchive/features/main/InAppUpdateCoordinator.kt`

Changes:
- Add lightweight condition to skip/defer check while `isImporting` or active upload queue exists.

Why:
- Avoid service binding and prompt checks during performance-sensitive window.

Risk:
- Low. User still gets check later.

## Phase 6: DB Query Churn Cleanup (secondary)

### 6.1 Avoid `getById + upsert` on update path
File:
- `app/src/main/java/net/opendasharchive/openarchive/core/repositories/EvidenceRepositoryImpl.kt`
- DAO additions in `EvidenceDao.kt`

Changes:
- Add targeted `@Update` / `@Query` update methods for known update paths.
- Use existence checks only when truly required.

### 6.2 Address N+1 mapping in `getQueue()`
File:
- `app/src/main/java/net/opendasharchive/openarchive/core/repositories/EvidenceRepositoryImpl.kt`

Changes:
- Join or batch fetch archive/vault references once per queue fetch.

Why:
- Reduces DB work and query callback spam.

Risk:
- Medium due to data layer changes.

## Rollout Strategy
1. Phase 1 + Phase 4 first (quick wins, minimal risk).
2. Phase 2 next (remove sync broadcast pressure).
3. Phase 3 after validating baseline improvement.
4. Phase 5 and 6 as follow-up hardening.

## Verification Matrix
After each phase, run:
1. Upload single image (IA).
2. Upload single image (WebDAV).
3. Upload large video (IA).
4. Upload large video (WebDAV).
5. Upload 10 mixed media in batch.
6. Open upload manager during active upload.
7. Navigate across Home pages while upload active.

Check for each:
- Frame skip count and Davey logs.
- Progress correctness and terminal status.
- No stuck UI states.
- No missing success/failure events.

## Instrumentation to Keep (short term)
Add temporary counters (debug-only):
- progress callbacks received (network layer)
- progress events emitted (Conduit)
- progress events applied (MainMediaViewModel)
- recompositions of media grid item (optional)

Remove or disable after validation.

## Out-of-scope (this plan)
- Full upload architecture rewrite.
- WorkManager migration of upload service.
- Backend protocol-level redesign.

## Execution Note
This plan is intentionally staged to avoid broad regressions. If Phase 1 and 2 eliminate most jank, Phase 3 can be minimized to targeted cleanup rather than a large state pipeline refactor.
