# Vault Credential Security Plan

This document tracks the active plan to remove plaintext vault secrets from Room and move IA/WebDAV secrets into secure storage.

## Active Plan

Primary implementation plan:
- `/Users/elelan/StudioProjects/OpenArchive/save-android-old/docs/plans/2026-02-13-vault-credential-security-phase1.md`

## Decisions Locked

1. `VaultEntity.password` will be removed now (breaking Room schema change is acceptable).
2. No DB-password fallback path is required.
3. During one-time Sugar -> Room migration, `Space.password` will be migrated directly to secure storage.
4. UI should not load persisted password values unless user is explicitly entering a new secret.

## Why Not Hash Passwords in DB?

Hashing is not suitable for IA/WebDAV credentials because authentication requires recovering the original secret for outbound requests.

- Hash: one-way, non-recoverable.
- Required here: reversible retrieval at runtime.

So we must use encryption-backed secure storage (Android Keystore), not hashing.
