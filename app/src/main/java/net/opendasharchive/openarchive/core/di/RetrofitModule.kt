package net.opendasharchive.openarchive.core.di

import android.content.Context
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.service.RetrofitAPI
import net.opendasharchive.openarchive.services.snowbird.service.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val retrofitModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single<HttpLoggingInterceptor>(named("snowbird_http_logger")) {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single<OkHttpClient>(named("snowbird_okhttp")) {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(get<HttpLoggingInterceptor>(named("snowbird_http_logger")))
            .build()
    }

    single<Retrofit>(named("snowbird_retrofit")) {
        Retrofit.Builder()
            .baseUrl("http://localhost:8080/api/")
            .client(get<OkHttpClient>(named("snowbird_okhttp")))
            .addConverterFactory(get<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single<RetrofitClient>(named("snowbird_retrofit_client")) {
        get<Retrofit>(named("snowbird_retrofit")).create(RetrofitClient::class.java)
    }

    single<ISnowbirdAPI>(named("snowbird_api")) {
        RetrofitAPI(
            context = get<Context>(),
            client = get(named("snowbird_retrofit_client"))
        )
    }
}
