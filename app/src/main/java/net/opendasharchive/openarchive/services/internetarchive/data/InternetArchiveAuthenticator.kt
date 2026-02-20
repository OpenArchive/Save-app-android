package net.opendasharchive.openarchive.services.internetarchive.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.core.domain.Credentials
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultAuthenticator
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.FormBody
import okhttp3.Request

private const val LOGIN_URI = "https://archive.org/services/xauthn?op=login"
private const val ARCHIVE_API_ENDPOINT = "https://archive.org"

class InternetArchiveAuthenticator(
    private val context: Context,
    private val json: Json,
) : VaultAuthenticator {

    override suspend fun authenticate(credentials: Credentials): Result<Vault> {
        if (credentials !is Credentials.InternetArchive) {
            return Result.failure(IllegalArgumentException("Invalid credentials type"))
        }

        val authDataResult = withContext(Dispatchers.IO) {
            SaveClient.get(context).enqueueResult(
                Request.Builder()
                    .url(LOGIN_URI)
                    .post(
                        FormBody.Builder()
                            .add("email", credentials.email)
                            .add("password", credentials.pass).build()
                    )
                    .build()
            ) { response ->
                val body = response.body?.string() ?: return@enqueueResult Result.failure(Exception("Empty response body"))
                val data = json.decodeFromString<InternetArchiveLoginResponse>(body)

                if (!data.success) {
                    return@enqueueResult Result.failure<LoginIntermediateData>(IllegalArgumentException(data.values.reason ?: "Unknown error"))
                }

                val auth = data.values.s3 ?: return@enqueueResult Result.failure<LoginIntermediateData>(Exception("S3 keys missing in response"))

                Result.success(
                    LoginIntermediateData(
                        access = auth.access,
                        secret = auth.secret,
                        screenName = data.values.screenname ?: "",
                        email = data.values.email ?: ""
                    )
                )
            }
        }

        return authDataResult.fold(
            onSuccess = { intermediate ->
                // Test connection (now outside the lambda, so suspension works)
                val testResult = testConnectionInternal(intermediate.access, intermediate.secret)
                if (testResult.isFailure) {
                    return Result.failure(testResult.exceptionOrNull() ?: Exception("Connection test failed"))
                }

                val metaData = InternetArchiveMetadata(
                    screenName = intermediate.screenName,
                    email = intermediate.email
                )

                Result.success(
                    Vault(
                        type = VaultType.INTERNET_ARCHIVE,
                        username = intermediate.access,
                        password = intermediate.secret,
                        displayName = intermediate.screenName,
                        metaData = json.encodeToString(InternetArchiveMetadata.serializer(), metaData),
                        host = ARCHIVE_API_ENDPOINT
                    )
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    private data class LoginIntermediateData(
        val access: String,
        val secret: String,
        val screenName: String,
        val email: String
    )

    override suspend fun testConnection(vault: Vault): Result<Unit> {
        return testConnectionInternal(vault.username, vault.password)
    }

    private suspend fun testConnectionInternal(access: String, secret: String): Result<Unit> = withContext(Dispatchers.IO) {
        SaveClient.get(context).enqueueResult(
            Request.Builder()
                .url(ARCHIVE_API_ENDPOINT)
                .method("GET", null)
                .addHeader("Authorization", "LOW $access:$secret")
                .build()
        ) { response ->
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(UnauthenticatedException())
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class InternetArchiveMetadata(
    val screenName: String,
    val email: String
)
