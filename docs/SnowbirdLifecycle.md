# Snowbird Lifecycle (Android ↔ Local Rust Server)

Quick reference for the startup/readiness and refresh issues seen in DWeb testing.

## Startup flow

1. `SnowbirdFragment` starts `SnowbirdService`.
2. `SnowbirdService` calls `SnowbirdBridge.initialize()` then `startServer(...)`.
3. Service marks connected only after both checks pass:
   - `GET http://localhost:8080/status`
   - `GET http://localhost:8080/health/ready`

## Why this matters

- `/status` means HTTP server is up.
- `/health/ready` means Veilid/Iroh/Blobs init is complete.
- Without the second check, UI can look connected while backend calls still fail.

## Refresh guardrails

- Filter refresh results to repos that belong to the selected group.
- Surface refresh failures by category:
  - `DHT_DISCOVERY` (for repo root hash lookup failures)
  - `PEER_DOWNLOAD` (for peer download failures)
  - `UNKNOWN`

## Relevant files

- `app/src/main/java/net/opendasharchive/openarchive/services/snowbird/service/SnowbirdService.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/snowbird/SnowbirdRepoViewModel.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/snowbird/SnowbirdGroupRepository.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/snowbird/SnowbirdRepoRepository.kt`
- `app/src/main/java/net/opendasharchive/openarchive/services/snowbird/SnowbirdFileRepository.kt`
