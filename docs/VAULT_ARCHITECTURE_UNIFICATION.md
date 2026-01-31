# Vault Architecture Unification

This document outlines the technical plan to streamline backend service logic for **Internet Archive (IA)**, **WebDAV**, and future storage providers. The goal is to eliminate boilerplate in the IA feature and decouple protocol logic in WebDAV, creating a unified "Service Layer" for all Vault operations.

## Backend Core Interfaces

To unify how the application interacts with different storage backends during authentication and configuration, we will introduce a standard `VaultAuthenticator`.

```kotlin
/**
 * Standard interface for authenticating and validating server connections.
 */
interface VaultAuthenticator {
    /**
     * Authenticates with the service and returns a Vault object populated with 
     * necessary keys, display names, and metadata.
     */
    suspend fun authenticate(credentials: Credentials): Result<Vault>

    /**
     * Validates an existing Vault connection (e.g., checking if keys are still valid).
     */
    suspend fun testConnection(vault: Vault): Result<Unit>
}

sealed class Credentials {
    data class WebDav(val url: String, val user: String, val pass: String) : Credentials()
    data class InternetArchive(val email: String, val pass: String) : Credentials()
}
```

## Standardizing Architectural Style

The project currently suffers from a "Style Mismatch": IA is overly formal (multi-layered Clean), while WebDAV is overly flat. We will adopt a unified **Service-Authenticator Pattern** for both.

### Unified Package Structure
We will move away from the deep nesting in IA and the flatness in WebDAV towards a standardized structure:

| Feature | New Unified Structure | Old IA Style | Old WebDAV Style |
| :--- | :--- | :--- | :--- |
| **Authentication** | `.../data/service/XAuthenticator.kt` | `.../infrastructure/repository/XRepository.kt` | Logic inside `ViewModel` |
| **Data Operations**| `.../data/repository/XRepository.kt` | `N/A` | `.../WebDavRepository.kt` |
| **API Models** | `.../data/model/XModels.kt` | `.../infrastructure/model/XModels.kt` | `N/A` |

### The "Authenticator" Pattern
The `Authenticator` implementation for each backend will be responsible for:
1.  **Protocol specifics**: Executing the raw OkHttp/Sardine calls.
2.  **Domain mapping**: Mapping the raw response to a core `Vault` object.
3.  **Validation**: Performing initial connection tests.

## Streamlining Strategy

### 1. Internet Archive (IA) Compression
The current IA implementation is multi-layered with redundant entities. We will flatten this:
- **Collapse Infrastructure**: Remove the `datasource/`, `mapping/`, and `repository/` sub-packages. Consolidate into a single `InternetArchiveAuthenticator` in a `data/` package.
- **Eliminate Redundant Models**: Remove the internal `InternetArchive` domain model. Use the core `Vault` model directly.
- **Unified Metadata**: Store IA-specific details (screen name, email) in the `Vault.metaData` JSON field.
- **Remove LocalSource**: Eliminate the memory-only `InternetArchiveLocalSource`.

### 2. WebDAV Protocol Decoupling
- **Implement Authenticator**: Move login and "Fix URL" logic from `WebDavLoginViewModel` into a new `WebDavAuthenticator`.
- **Standardize Repository**: Refactor `WebDavRepository` to use `Result<T>` and remove the dependency on `Context` where possible.

### 3. Metadata Management
Standardize how specialized backend data is stored in the `Vault` domain model.

```kotlin
// Helper for managing IA metadata in Vault.metaData field
data class InternetArchiveMetadata(
    val screenName: String,
    val email: String
) {
    fun toJson(gson: Gson): String = gson.toJson(this)
    companion object {
        fun fromVault(vault: Vault, gson: Gson): InternetArchiveMetadata = ...
    }
}
```

## Implementation Roadmap

### Phase 1: Core Definitions
- Define `VaultAuthenticator` and `Credentials`.
- Move `CustomTextField` and shared UI components to a common package.

### Phase 2: Implementation Consolidation
- Create `WebDavAuthenticator` and `InternetArchiveAuthenticator`.
- Simplify IA infrastructure by removing redundant layers.

### Phase 3: Dependency Injection Setup
Group all authenticators in a new `VaultModule` (Koin):
```kotlin
val vaultModule = module {
    single<WebDavAuthenticator> { WebDavAuthenticator(get(), get()) }
    single<InternetArchiveAuthenticator> { InternetArchiveAuthenticator(get(), get()) }
}
```
