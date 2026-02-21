# Vault Credential Security Phase 1 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove plaintext IA/WebDAV secrets from Room by deleting `VaultEntity.password`, storing secrets only in keystore-backed secure storage, and migrating Sugar passwords directly into secure storage during one-time migration.

**Architecture:** Split vault persistence into two stores: non-secret metadata in Room (`VaultEntity`) and secret material in `VaultSecureStorage` keyed by `vaultId`. UI can collect credentials only during login/edit entry, but persisted secrets are never re-hydrated into UI state unnecessarily. Runtime upload/auth paths obtain secrets through repository/domain APIs, not direct DB columns.

**Tech Stack:** Kotlin, Room, Android Keystore, `androidx.security:security-crypto` (`EncryptedSharedPreferences`), WorkManager (`MigrationWorker`), Koin DI.

---

## Scope and Non-Goals

### In scope
- Remove `password` from `VaultEntity`.
- Persist IA secret/WebDAV password only in secure storage.
- Update login/save/update/delete flows for vault credentials.
- Perform one-time Sugar -> Room credential migration into secure storage.
- Avoid loading stored credentials into UI state except when user explicitly enters a new value.

### Out of scope (Phase 1)
- Media-at-rest encryption.
- Full SQLCipher DB encryption.
- Biometric step-up UX.
- Backward fallback to DB password column (explicitly not needed per current product decision).

---

## Task 1: Add Secure Credential Storage Abstraction

**Files:**
- Create: `app/src/main/java/net/opendasharchive/openarchive/core/security/VaultCredentialStore.kt`
- Create: `app/src/main/java/net/opendasharchive/openarchive/core/security/EncryptedVaultCredentialStore.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/di/*.kt` (Koin module binding)

**Step 1: Define interface**
- `suspend fun putSecret(vaultId: Long, secret: String)`
- `suspend fun getSecret(vaultId: Long): String?`
- `suspend fun deleteSecret(vaultId: Long)`
- `suspend fun hasSecret(vaultId: Long): Boolean`

**Step 2: Implement `EncryptedSharedPreferences` store**
- Single encrypted prefs file (for example `vault_secrets`).
- Key pattern: `vault_secret_<vaultId>`.
- Never log secret values.

**Step 3: Bind in DI**
- Register singleton implementation for repository + migration worker usage.

**Step 4: Commit**
- `feat(security): add keystore-backed vault credential store`

---

## Task 2: Remove Password from Room Schema

**Files:**
- Modify: `app/src/main/java/net/opendasharchive/openarchive/db/VaultEntity.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/core/domain/mappers/Mappers.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/db/AppDatabase.kt`
- Modify: `app/schemas/**` (regenerated Room schema JSON)

**Step 1: Remove DB column**
- Delete `password` from `VaultEntity`.

**Step 2: Update mappers**
- `VaultEntity.toDomain()` should map non-secret fields only.
- `Vault.toVaultEntity()` should not include secret fields.

**Step 3: Bump Room schema version**
- Increment DB version in `AppDatabase`.
- Add migration path only if needed for local dev data continuity.

**Step 4: Re-generate schema files**
- Ensure Room schema export is updated.

**Step 5: Commit**
- `refactor(db): remove vault password from room entity`

---

## Task 3: Add Domain-Level Credential Model for Runtime Use

**Files:**
- Create: `app/src/main/java/net/opendasharchive/openarchive/core/domain/VaultAuth.kt` (or similar)
- Modify: `app/src/main/java/net/opendasharchive/openarchive/core/repositories/SpaceRepository.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/core/repositories/VaultRepositoryImpl.kt`

**Step 1: Introduce runtime auth DTO**
- `data class VaultAuth(val vaultId: Long, val username: String, val secret: String, val type: VaultType)`

**Step 2: Extend repository contract**
- Add `suspend fun getVaultAuth(vaultId: Long): VaultAuth?`
- Keep existing `getSpaceById()` for non-secret metadata usage.

**Step 3: Implement in `VaultRepositoryImpl`**
- Resolve username from Room + secret from `VaultCredentialStore`.
- Return null if secret missing.

**Step 4: Commit**
- `feat(security): add runtime vault auth retrieval API`

---

## Task 4: Update Vault Save/Update/Delete Flows

**Files:**
- Modify: `app/src/main/java/net/opendasharchive/openarchive/core/repositories/VaultRepositoryImpl.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/webdav/presentation/login/WebDavLoginViewModel.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/internetarchive/presentation/login/InternetArchiveLoginViewModel.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/webdav/presentation/detail/WebDavDetailViewModel.kt`

**Step 1: `addSpace(vault)` path**
- Persist metadata row first (without secret).
- Persist secret in `VaultCredentialStore` using returned `vaultId`.
- If secure-store write fails, fail operation and surface error.

**Step 2: `updateSpace(vaultId, vault)` path**
- Update metadata in Room.
- Update secret only when a new non-blank secret is supplied.

**Step 3: `deleteSpace(id)` path**
- Delete Room row.
- Delete secret from secure store.

**Step 4: UI minimization**
- Do not load persisted password into `WebDavDetailState` on screen load.
- Treat password field as “enter new password” only.

**Step 5: Commit**
- `feat(security): route vault secret lifecycle through secure store`

---

## Task 5: Update Runtime Credential Consumers

**Files:**
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/webdav/data/WebDavConduit.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/internetarchive/data/IaConduit.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/webdav/data/WebDavRepository.kt`
- Modify: `app/src/main/java/net/opendasharchive/openarchive/upload/UploadService.kt` (if needed for auth-precheck errors)
- Modify: `app/src/main/java/net/opendasharchive/openarchive/services/Conduit.kt` (if shared helper added)

**Step 1: Replace `vault.password` usage**
- Fetch auth via `spaceRepository.getVaultAuth(vaultId)`.

**Step 2: Wire auth into clients**
- WebDAV sardine + IA auth header should use `VaultAuth.secret`.

**Step 3: Secret-missing handling**
- If `getVaultAuth()` returns null, fail gracefully with clear credential error.

**Step 4: Commit**
- `refactor(auth): migrate upload/network consumers to vault auth DTO`

---

## Task 6: Sugar -> Room One-Time Credential Migration (No Fallback)

**Files:**
- Modify: `app/src/main/java/net/opendasharchive/openarchive/db/MigrationWorker.kt`

**Step 1: In `migrateSpaces()`**
- Create `VaultEntity` without password.
- Persist secret from `SugarSpace.password` into `VaultCredentialStore` immediately using the migrated `vaultId`.

**Step 2: Migration failure policy**
- If secure-store write fails for a vault, fail migration run (`Result.retry()`) so app does not continue with partially migrated secrets.

**Step 3: Commit**
- `feat(migration): move sugar vault secrets to secure storage during room migration`

---

## Task 7: Tests

**Files:**
- Create/Modify tests under:
  - `app/src/test/java/.../core/repositories/VaultRepositoryImplTest.kt`
  - `app/src/test/java/.../db/MigrationWorkerTest.kt`
  - `app/src/androidTest/java/.../security/EncryptedVaultCredentialStoreTest.kt` (or Robolectric equivalent)

**Test cases:**
1. `addSpace()` stores metadata in DB and secret in secure store.
2. `getVaultAuth()` returns username + secret; `getSpaceById()` does not expose persisted secret usage in UI paths.
3. `updateSpace()` with blank password does not wipe existing stored secret.
4. `deleteSpace()` removes secure entry.
5. `MigrationWorker` migrates Sugar password -> secure store and leaves no DB plaintext field dependency.
6. Upload conduits fail predictably when secret missing.

**Commit:**
- `test(security): add vault credential secure storage coverage`

---

## Task 8: Manual Verification Checklist

1. Fresh install:
- Add IA and WebDAV vaults.
- Confirm login succeeds and uploads succeed.
- Confirm no password column exists in Room schema.

2. Migration path:
- Start with Sugar data containing passwords.
- Run migration worker.
- Confirm vault metadata exists in Room and auth still works.

3. UI exposure:
- Open WebDAV details screen.
- Confirm password is not prefilled from storage.

4. Deletion:
- Remove vault.
- Confirm secret removed from secure store (instrumentation check/logical verification).

---

## Risk Controls

- Keep secrets out of logs, analytics payloads, exceptions.
- Treat secure-store write/read failures as credential errors, not crashes.
- Run upload smoke test on both backends before release branch cut.

---

## Hashing vs Encryption Decision

Do **not** hash vault passwords for this use case.

- Hashing is one-way; you cannot recover the original secret.
- IA/WebDAV authentication requires the original secret on each request.
- Therefore secrets must be stored with **reversible encryption** (keystore-backed), not hashes.

Use hashing only for verification-only secrets (e.g., user login verification), which is not this case.
