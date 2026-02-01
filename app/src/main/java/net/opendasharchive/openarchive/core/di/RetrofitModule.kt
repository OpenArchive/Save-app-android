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

    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("http://localhost:8080/api/")
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single<RetrofitClient> { get<Retrofit>().create(RetrofitClient::class.java) }

    single<ISnowbirdAPI>(named("retrofit")) {
        RetrofitAPI(
            context = get<Context>(),
            client = get<RetrofitClient>()
        )
    }
}