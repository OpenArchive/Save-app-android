package net.opendasharchive.openarchive.services.storacha.di

import com.google.gson.GsonBuilder
import net.opendasharchive.openarchive.services.storacha.network.AuthInterceptor
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.BridgeUploader
import net.opendasharchive.openarchive.services.storacha.util.SecureStorage
import net.opendasharchive.openarchive.services.storacha.util.SessionManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaAccountDetailsViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaBrowseSpacesViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaDIDAccessViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaEmailVerificationSentViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaLoginViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaViewDIDsViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val storachaModule =
    module {

        // Secure storage for Storacha accounts (used by AccountManager)
        single(qualifier = named("accountsStorage")) {
            SecureStorage(get(), "storacha_accounts")
        }

        // Secure storage for DID keys (used by SessionManager)
        single(qualifier = named("keysStorage")) {
            SecureStorage(get(), "storacha_did_keys")
        }

        // Account manager
        single {
            StorachaAccountManager(get())
        }

        // Session manager (depends on API service, account manager, and keys storage)
        single {
            SessionManager(get(), get(), get(named("keysStorage")))
        }

        // Logging interceptor for API calls (HEADERS level to avoid OOM on large files)
        single<HttpLoggingInterceptor>(named("storacha_http_logger")) {
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
        }

        // Auth interceptor (uses lazy provider to break circular dependency)
        single {
            AuthInterceptor { get() }
        }

        // OkHttp client with interceptors (for general API calls)
        single<OkHttpClient>(named("storacha_okhttp")) {
            OkHttpClient
                .Builder()
                .addInterceptor(get<HttpLoggingInterceptor>(named("storacha_http_logger")))
                .addInterceptor(get<AuthInterceptor>())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
                .writeTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
                .build()
        }

        // OkHttp client for file uploads - NO body logging to prevent OOM
        single(qualifier = named("uploadClient")) {
            OkHttpClient
                .Builder()
                // No logging interceptor - prevents OOM on large file uploads
                .addInterceptor(get<AuthInterceptor>())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build()
        }

        single(named("storacha_gson")) {
            GsonBuilder().create()
        }

        single<Retrofit>(named("storacha_retrofit")) {
            Retrofit
                .Builder()
                .baseUrl("http://save-storacha.staging.hypha.coop:3000/") // Change to actual API base URL
                .client(get<OkHttpClient>(named("storacha_okhttp")))
                .addConverterFactory(GsonConverterFactory.create(get(named("storacha_gson"))))
                .build()
        }

        single<StorachaApiService> {
            get<Retrofit>(named("storacha_retrofit")).create(StorachaApiService::class.java)
        }

        // BridgeUploader uses the upload client without body logging
        single { BridgeUploader(get(named("uploadClient"))) }

        viewModelOf(::StorachaLoginViewModel)
        viewModelOf(::StorachaBrowseSpacesViewModel)
        viewModelOf(::StorachaMediaViewModel)
        viewModelOf(::StorachaViewDIDsViewModel)
        viewModelOf(::StorachaDIDAccessViewModel)
        viewModelOf(::StorachaAccountDetailsViewModel)
        viewModel { (application: android.app.Application, sessionId: String) ->
            StorachaEmailVerificationSentViewModel(
                application,
                get(),
                sessionId,
            )
        }
    }
