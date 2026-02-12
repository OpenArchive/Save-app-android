In this system, groups, repos, and files are primarily differentiated by cryptographic keys and hashes rather than names, which allows multiple entities to share the same name while remaining unique in the database.

### 1. Groups
*   **Primary Key (Unique Identifier)**: Each group has a `key`, which is a base64-encoded Veilid `RecordKey` (a 32-byte cryptographic public key).
*   **Differentiation**: Even if two groups have the same `name` (e.g., "SaveWeb"), they will have distinct `key` values and `uri` strings.
*   **Implementation**: In `src/models.rs`, the `SnowbirdGroup` struct uses `group.id().to_string()` to populate the `key` field.
    ```rust
    pub struct SnowbirdGroup {
        pub key: String, // Unique cryptographic identifier
        pub name: Option<String>,
        pub uri: String,
    }
    ```

### 2. Repositories (Repos)
*   **Primary Key (Unique Identifier)**: Similar to groups, each repository has a `key` that is a base64-encoded cryptographic `RecordKey`.
*   **Differentiation**: Repos are unique across the entire system by their key. Within a group, they are also distinguished by this key.
*   **Implementation**: In `src/models.rs`, the `SnowbirdRepo` struct uses `repo.id().to_string()` for its `key`.
    ```rust
    pub struct SnowbirdRepo {
        pub key: String, // Unique cryptographic identifier
        pub name: String,
        pub can_write: bool,
    }
    ```

### 3. Files
*   **Identifiers**: Files are identified by two main attributes:
    1.  **`name`**: The filename string, which is unique within a specific repository.
    2.  **`hash`**: An Iroh blob hash (BLAKE3), which is a unique identifier for the file's content.
*   **Implementation**: In `src/media.rs`, the response for listing files includes both:
    ```json
    {
      "name": "example.txt",
      "hash": "...", // Unique content hash
      "is_downloaded": true
    }
    ```

### Summary of Differentiation
| Entity | Primary Identifier | Type | Uniqueness |
| :--- | :--- | :--- | :--- |
| **Group** | `key` | Cryptographic Public Key (Base64) | Global |
| **Repo** | `key` | Cryptographic Public Key (Base64) | Global |
| **File** | `hash` | BLAKE3 Content Hash | Global (per content) |
| **File** | `name` | String | Local (per Repo) |

Because the API and the underlying database use these cryptographic keys/hashes as the primary lookup mechanism (see `create_veilid_cryptokey_from_base64` in `src/utils.rs`), names are purely for display and do not cause collisions.