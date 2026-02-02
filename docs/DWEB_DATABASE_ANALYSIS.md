# DWeb Database Architecture Analysis

This document evaluates three strategies for integrating DWeb (formerly Snowbird) metadata into our Room database, considering the potential growth of Veilid rust backend features.

---

## Option 1: Fully Merged (Unified Entities)
*Current approach in [DWEB_INTEGRATION_PLAN.md](file:///Users/elelan/StudioProjects/OpenArchive/save-android-old/docs/DWEB_INTEGRATION_PLAN.md)*

**Structure**: Add nullable columns (e.g., `vaultKey`, `archiveHash`) directly to `VaultEntity`, `ArchiveEntity`, and `EvidenceEntity`.

### Pros
- **Simplest Querying**: No SQL joins required.
- **Unified UI**: Existing ViewModels and Repositories work without modification.
- **Minimal Boilerplate**: One DAO, one Mapper, one Repository for "Archives" regardless of type.

### Cons
- **Table Bloat**: If DWeb/Veilid required 10-20 specific fields (keys, signatures, sync states), every row for WebDAV/IA would have empty columns.
- **Schema Fragility**: Frequent changes to the DWeb spec require migrations for the core project tables.

---

## Option 2: Fully Separate (Distinct Tables)
**Structure**: Create `DwebVaultEntity`, `DwebArchiveEntity`, and `DwebEvidenceEntity`.

### Pros
- **Isolation**: DWeb evolution never breaks WebDAV or Internet Archive.
- **Clean Schema**: Only the fields needed for DWeb are present.

### Cons
- **UI Fragmentation**: Harder to show "All Folders" in one list. Requires complex `UNION` queries or separate loaders.
- **Code Duplication**: Many fields (name, description, timestamp) are duplicated, requiring duplicated domain logic.

---

## Option 3: Hybrid (Core + Extension Tables) ★ Recommended for High Complexity
**Structure**: Keep core identity in main tables; Move DWeb-specific metadata to 1-to-1 extension tables (e.g., `VaultDwebMetadata`).

### Relationship Example
- `VaultEntity` (ID: 1, Name: "My DWeb Space")
- `VaultDwebEntity` (VaultID: 1, DwebKey: "xyz...", Uri: "raven://...")

### Pros
- **Scalable Metadata**: We can add as many Veilid-specific rust backend fields as needed without affecting the core `VaultEntity`.
- **Unified UI**: We still have a single "Vault" or "Archive" identity. We only join when we specifically need DWeb details.
- **Type Safety**: Clean separation between "Common" and "Backend-Specific" data.

### Cons
- **Joins**: Slightly more complex SQL in the Room DAOs.
- **Relation Management**: Requires `Transaction` when deleting a vault to ensure the extension row is also cleaned up (though Room's `CASCADE` handles this easily).

---

## Decision Matrix

| Feature | Merged (Opt 1) | Separate (Opt 2) | Hybrid (Opt 3) |
| :--- | :--- | :--- | :--- |
| **Simplicity** | High | Medium | Medium |
| **UI Integration**| Excellent | Poor | Excellent |
| **Scalability** | Low | High | High |
| **Schema Health** | Poor | Excellent | Excellent |

---

## Recommendation

If we expect the **Veilid rust backend** to introduce many specific fields (like multiple peer keys, local cache states, or unique IDs), **Option 3 (Hybrid)** is the superior architecture. It keeps our core "Archives" clean while providing a dedicated home for DWeb's unique requirements.
