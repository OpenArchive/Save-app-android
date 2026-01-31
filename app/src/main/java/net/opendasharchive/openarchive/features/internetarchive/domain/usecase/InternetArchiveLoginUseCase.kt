package net.opendasharchive.openarchive.features.internetarchive.domain.usecase

import com.google.gson.Gson
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager

class InternetArchiveLoginUseCase(
    private val repository: InternetArchiveRepository,
    private val gson: Gson,
    private val spaceRepository: SpaceRepository,
    private val analyticsManager: AnalyticsManager,
) {

    suspend operator fun invoke(email: String, password: String): Result<Long> =
        repository.login(email, password).mapCatching { response ->

            val auth = response.auth
            repository.testConnection(auth).getOrThrow()

            val metaDataJson = gson.toJson(response.meta)

            val vault = Vault(
                type = VaultType.INTERNET_ARCHIVE,
                username = auth.access,
                password = auth.secret,
                displayName = response.meta.screenName,
                metaData = metaDataJson,
                host = "https://archive.org"
            )

            val vaultId = spaceRepository.addSpace(vault)
            spaceRepository.setCurrentSpace(vaultId)

            // Track backend configuration
            analyticsManager.trackBackendConfigured(
                backendType = VaultType.INTERNET_ARCHIVE.friendlyName,
                isNew = true
            )

            vaultId
        }

}
