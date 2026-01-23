package net.opendasharchive.openarchive.features.internetarchive.domain.usecase

import com.google.gson.Gson
import net.opendasharchive.openarchive.db.sugar.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager

class InternetArchiveLoginUseCase(
    private val repository: InternetArchiveRepository,
    private val gson: Gson,
    private val space: Space,
    private val analyticsManager: AnalyticsManager,
) {

    suspend operator fun invoke(email: String, password: String): Result<InternetArchive> =
        repository.login(email, password).mapCatching { response ->

            response.auth.let { auth ->
                repository.testConnection(auth).getOrThrow()
                space.username = auth.access
                space.password = auth.secret
            }

            // TODO: use local data source for database
            space.metaData = gson.toJson(response.meta)

            // Check if this is a new backend or existing one
            val isNewBackend = space.id == null || space.id == 0L

            space.save()

            Space.current = space

            // Track backend configuration
            analyticsManager.trackBackendConfigured(
                backendType = Space.Type.INTERNET_ARCHIVE.friendlyName,
                isNew = isNewBackend
            )

            response
        }

}
