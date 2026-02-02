# DWeb Integration Plan (Hybrid Approach)

This plan outlines the integration of DWeb features (formerly Snowbird) into the Room database using a **Hybrid Extension Table** approach. This ensures our core database remains clean while providing a scalable home for DWeb and Veilid-specific metadata.

## User Review Required

> [!IMPORTANT]
> **No Legacy Migration**: DWeb data will not be migrated from legacy Sugar ORM tables. It will be implemented as a fresh feature within the Room database.

## Architecture

We will use a 1-to-1 extension pattern. Core identity properties (name, id, timestamps) stay in the main tables. DWeb-specific properties (keys, hashes, sync status) go into extension tables.

---

## Proposed Changes

### Database Layer [DWeb/Room]

#### [NEW] `VaultDwebEntity.kt`
- `vaultId: Long` (Primary Key, Foreign Key to `VaultEntity.id`)
- `vaultKey: String` (Unique DWeb identifier)

#### [NEW] `ArchiveDwebEntity.kt`
- `archiveId: Long` (Primary Key, Foreign Key to `ArchiveEntity.id`)
- `archiveKey: String`
- `archiveHash: String`
- `permissions: ArchivePermission`

#### [NEW] `EvidenceDwebEntity.kt`
- `evidenceId: Long` (Primary Key, Foreign Key to `EvidenceEntity.id`)
- `isDownloaded: Boolean`

#### [NEW] `ArchivePermission.kt`
```kotlin
enum class ArchivePermission {
    READ_ONLY,
    READ_WRITE
}
```

#### [MODIFY] `AppDatabase.kt` / `VaultDao.kt` / etc.
- Register new entities.
- Add queries that join core tables with their DWeb extensions.

---

### Domain Layer Updates

#### [MODIFY] `Vault.kt` / `Archive.kt` / `Evidence.kt`
- Add an optional `dwebMetadata` field or integrate the specific fields if they are commonly used.

---

### Mapper Updates

#### [MODIFY] `Mappers.kt`
- Update `toDomain` to take both the core entity and its optional DWeb extension entity.
- Update `toEntity` to produce both parts of the pair.

---

### Repository Updates

#### [MODIFY] `VaultRepositoryImpl.kt` (and others)
- When saving a `Vault`, if it contains DWeb metadata, save to both `VaultEntity` and `VaultDwebEntity` within a transaction.

## Verification Plan

### Automated Tests
- Verify `CASCADE` deletion: Deleting a `VaultEntity` must delete its `VaultDwebEntity`.
- Verify unified queries: Ensure `getSpaces()` still returns all spaces, correctly populating DWeb metadata where present.

### Manual Verification
- Create a DWeb vault and verify metadata is stored in the extension table.
- Verify IA and WebDAV vaults still function without any DWeb metadata rows.
