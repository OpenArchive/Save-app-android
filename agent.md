# Migration Task Board

## Immediate (per user)
- Pass `projectId` into `MainMediaViewModel` via Koin parameters/keys; ensure `MainMediaScreen` receives the current selected project from `HomeViewModel` (pager/drawer/tab changes flow HomeViewModel → composable → media VM).
- Make `HomeViewModel` the single source of truth for spaces/projects/pager; connect both HomeViewModel and MainMediaViewModel to Sugar-backed repositories (remove direct `.save()`/`.delete()` in UI/VM).

## Short-Term (Main screen migration)
- Re-establish upload progress plumbing with a Flow/UploadEvent bridge (replacing BroadcastManager listeners in Compose).
- Move selection state to UI-only (no persisted `Media.selected`); handle folder bar events (focus/options) via events.
- Ensure drawer/empty-states behavior matches legacy (disable drawer when no space; space switch reloads projects and selects first project).
- Coordinate FAB actions from HomeViewModel (pending add when on Settings, lastMediaIndex tracking).

## From migration plan (remaining)
- Define repository interfaces (Space/Project/Collection/Media/Upload) and Sugar implementations; later swap to Room.
- Compose-only main shell: single Scaffold with top bar, right drawer, bottom bar+FAB, pager (projects + Settings), per-project MediaListScreen, Upload Manager sheet.
- Integrate UploadEvent bus and upload manager sheet pause/resume logic.
- Plan Room-ready data layer (mappers, domain models) and remove Sugar usages after cutover.***
