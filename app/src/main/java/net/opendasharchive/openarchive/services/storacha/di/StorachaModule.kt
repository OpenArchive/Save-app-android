package net.opendasharchive.openarchive.services.storacha.di

import com.google.gson.GsonBuilder
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.BridgeUploader
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaBrowseSpacesViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaEmailVerificationSentViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaLoginViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaViewDIDsViewModel
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaDIDAccessViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val storachaModule =
    module {

        single {
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        }

        single {
            OkHttpClient.Builder().addInterceptor(get<HttpLoggingInterceptor>()).build()
        }

        single {
            GsonBuilder().create()
        }

        single {
            Retrofit
                .Builder()
                .baseUrl("http://192.168.0.55:3000/") // Change to actual API base URL
                .client(get())
                .addConverterFactory(GsonConverterFactory.create(get()))
                .build()
        }

        single<StorachaApiService> {
            get<Retrofit>().create(StorachaApiService::class.java)
        }

        single { BridgeUploader(get()) }

        viewModelOf(::StorachaLoginViewModel)
        viewModelOf(::StorachaBrowseSpacesViewModel)
        viewModelOf(::StorachaMediaViewModel)
        viewModelOf(::StorachaViewDIDsViewModel)
        viewModelOf(::StorachaDIDAccessViewModel)
        viewModel { (sessionId: String) ->
            StorachaEmailVerificationSentViewModel(
                get(),
                sessionId,
            )
        }
    }
