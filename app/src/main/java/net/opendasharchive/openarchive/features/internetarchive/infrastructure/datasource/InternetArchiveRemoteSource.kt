package net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource

import net.opendasharchive.openarchive.features.internetarchive.InternetArchiveGson
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginRequest
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginResponse
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.internetarchive.IaConduit.Companion.ARCHIVE_API_ENDPOINT
import okhttp3.FormBody
import okhttp3.Request

private const val LOGIN_URI = "https://archive.org/services/xauthn?op=login"

class InternetArchiveRemoteSource(
    private val client: SaveClient,
    private val gson: InternetArchiveGson
) {
    suspend fun login(request: InternetArchiveLoginRequest): Result<InternetArchiveLoginResponse> =
        client.enqueue(
            Request.Builder()
                .url(LOGIN_URI)
                .post(
                    FormBody.Builder()
                        .add("email", request.email)
                        .add("password", request.password).build()
                )
                .build()
        ).map { response ->
            gson.fromJson(
                response.body?.string(),
                InternetArchiveLoginResponse::class.java
            )
        }

    suspend fun testConnection(auth: InternetArchive.Auth): Result<Boolean> =
        client.enqueue(
            Request.Builder()
                .url(ARCHIVE_API_ENDPOINT)
                .method("GET", null)
                .addHeader("Authorization", "LOW ${auth.access}:${auth.secret}")
                .build()
        ).map { response ->
            response.isSuccessful
        }
}
