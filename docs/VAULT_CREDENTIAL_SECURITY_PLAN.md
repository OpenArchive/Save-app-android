# Vault Credential Security Plan

This document outlines the proposed changes to secure sensitive vault credentials (usernames/passwords) in the OpenArchive application. Currently, these are stored in plaintext within the Room database, which is a significant security risk.

## User Review Required

> [!WARNING]
> **Breaking Change (Internal)**: This will change how passwords are persisted. We must ensure a robust migration for existing vaults so users are not logged out or lose access to their servers.

## Problem Analysis

The `VaultEntity` currently stores the `password` field as a plaintext `String`. While Room (SQLite) is private to the app, it is not encrypted by default. Any rooted device, forensic tool, or accidental database backup would expose these credentials.

## Proposed Solution: Secure Storage Integration

We will move from plaintext database storage to **Android Keystore-backed secure storage**.

### 1. New Component: `VaultSecureStorage`

We will introduce a utility class using the `androidx.security:security-crypto` library.

- **Storage Method**: `EncryptedSharedPreferences`.
- **Encryption**: Uses AES-256 GCM for data encryption and RSA for key wrapping, backed by the Android Keystore.
- **Key Strategy**: Credentials will be keyed by the `vaultId` and `type` to ensure uniqueness.

### 2. Database Changes

#### [MODIFY] [VaultEntity.kt](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/db/VaultEntity.kt)
- Remove the `password` column from the database.
- (Optional) Add a `hasSecurePassword: Boolean` flag to explicitly track if credentials have been moved to secure storage.

### 3. Repository & Data Logic

#### [MODIFY] [VaultRepositoryImpl.kt](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/app/src/main/java/net/opendasharchive/openarchive/core/repositories/VaultRepositoryImpl.kt)
- **`getSpaces()` / `getSpaceById()`**: After fetching from `VaultDao`, the repository will fetch the password from `VaultSecureStorage` and populate the `Vault` domain object.
- **`addSpace()` / `updateSpace()`**: Before saving to `VaultDao`, the password will be stripped from the entity and saved into `VaultSecureStorage`.

### 4. Migration Strategy

When the app starts or a vault is first accessed:
1. Check if the `VaultEntity` has a non-empty `password` field in the database.
2. If yes, move that password to `VaultSecureStorage`.
3. Clear the `password` field in the `VaultEntity` and update the database row.
4. All subsequent writes will bypass the database password column.

## Verification Plan

### Automated Tests
- Unit test for `VaultSecureStorage` to ensure encryption/decryption works correctly.
- Integration test for `VaultRepositoryImpl` to verify the "Fetch from DB + Fetch from SecureStorage" flow.

### Manual Verification
- Add a new vault and verify via a database inspector that the `password` column is empty.
- Verify that uploads still work (indicating the conduits are successfully receiving the password from the repository).
