# Vault Architecture Unification

This document outlines the technical plan to streamline backend service logic for **Internet Archive (IA)**, **WebDAV**, and future storage providers. The goal is to eliminate boilerplate and unify the architectural style and data serialization across the project.

## Project Standardization

To ensure consistency and type safety across all storage backends, we are adopting the following standards:

1.  **Serialization**: `kotlinx.serialization` is the mandatory standard for all API models and metadata. **Gson is to be removed** from the backend service layer.
2.  **Architecture**: Both IA and WebDAV will follow the **Service-Authenticator Pattern**, where authentication and service-specific metadata extraction are decoupled from UI and data persistence.

## Unified Package Structure

Both features will follow this exact structure, eliminating deep sub-packages like `datasource`, `mapping`, or `infrastructure`:

```text
net.opendasharchive.openarchive.features.[feature_name]
├── data
│   ├── [Feature]Authenticator.kt  # Authentication & Connection testing
│   ├── [Feature]Repository.kt     # Data operations (e.g., listing folders)
│   └── [Feature]Models.kt         # @Serializable API request/response models
└── presentation                   # ViewModels, Screens, and States
```

## Backend Core Interfaces

```kotlin
/**
 * Standard interface for authenticating and validating server connections.
 */
interface VaultAuthenticator {
    suspend fun authenticate(credentials: Credentials): Result<Vault>
    suspend fun testConnection(vault: Vault): Result<Unit>
}

@Serializable
sealed class Credentials {
    @Serializable
    data class WebDav(val url: String, val user: String, val pass: String) : Credentials()
    @Serializable
    data class InternetArchive(val email: String, val pass: String) : Credentials()
}
```

## Data Serialization (Kotlinx)

Models will move from Gson to Kotlinx Serialization. Example for IA:

```kotlin
@Serializable
data class InternetArchiveLoginResponse(
    val success: Boolean,
    val values: Values,
    val version: Int
) {
    @Serializable
    data class Values(
        val s3: S3? = null,
        val screenname: String? = null,
        val email: String? = null,
        @SerialName("itemname") val itemName: String? = null,
        val reason: String? = null
    )

    @Serializable
    data class S3(
        val access: String,
        val secret: String
    )
}
```

## Streamlining Strategy

### 1. Internet Archive (IA) Flattening
- **Consolidate Data Layer**: Remove `datasource/`, `mapping/`, `domain/model/`, and `infrastructure/repository/`. Move logic into `InternetArchiveAuthenticator` and `InternetArchiveRepository` directly under the `data/` package.
- **Remove Redundant Entities**: Use the core `Vault` model directly. Store S3 keys and user details in `Vault.metaData` as a serialized JSON string.
- **Stateless Authentication**: Remove `InternetArchiveLocalSource`. Use `SpaceRepository` as the single source of truth for accounts.

### 2. WebDAV Clean-up
- **Decouple Logic**: Extract auth logic from `WebDavLoginViewModel` into `WebDavAuthenticator`.
- **Standardize Repository**: Refactor `WebDavRepository` to implement the same `data/Repository` pattern, removing direct dependencies on `Context`.

## Implementation Roadmap

### Phase 1: Core & Models
- Define `VaultAuthenticator` and `Credentials`.
- Migrate all IA and WebDAV models to `@Serializable`.

### Phase 2: Implementation Refactoring
- Rename and move IA files to the new `data/` structure.
- Implement `WebDavAuthenticator` and extract logic from WebDAV ViewModels.

### Phase 3: Dependency Injection
Group all authenticators in a new `VaultModule` (Koin):
```kotlin
val vaultModule = module {
    single<VaultAuthenticator>(named("WebDav")) { WebDavAuthenticator(get(), get()) }
    single<VaultAuthenticator>(named("IA")) { InternetArchiveAuthenticator(get(), get()) }
}
```
