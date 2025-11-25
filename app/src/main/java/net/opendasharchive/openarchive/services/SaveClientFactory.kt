package net.opendasharchive.openarchive.services

import android.content.Context
import okhttp3.OkHttpClient

/**
 * Factory interface for creating OkHttpClient instances with authentication.
 * This abstraction allows injecting client creation without directly depending on Context.
 */
interface SaveClientFactory {
    /**
     * Creates an OkHttpClient with optional authentication credentials.
     *
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @return Configured OkHttpClient instance
     */
    suspend fun createClient(username: String = "", password: String = ""): OkHttpClient
}

/**
 * Default implementation of SaveClientFactory that uses SaveClient.
 */
class SaveClientFactoryImpl(
    private val context: Context
) : SaveClientFactory {
    override suspend fun createClient(username: String, password: String): OkHttpClient {
        return SaveClient.get(context, username, password)
    }
}
