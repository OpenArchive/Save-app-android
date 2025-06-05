package net.opendasharchive.openarchive.features.internetarchive

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.InternetArchiveLoginUseCase
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.ValidateLoginCredentialsUseCase
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource.InternetArchiveLocalSource
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource.InternetArchiveRemoteSource
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping.InternetArchiveMapper
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.features.internetarchive.presentation.details.InternetArchiveDetailsViewModel
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

typealias InternetArchiveGson = Gson

val internetArchiveModule = module {
    single<InternetArchiveGson> {
        Gson().newBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }
    factory { ValidateLoginCredentialsUseCase() }
    factory { InternetArchiveRemoteSource(get(), get()) }
    single { InternetArchiveLocalSource() }
    factory { InternetArchiveMapper() }
    factory { InternetArchiveRepository(get(), get(), get()) }
    factory { args -> InternetArchiveLoginUseCase(get(), get(), args.get()) }
    viewModel { args -> InternetArchiveDetailsViewModel(get(), args.get()) }
    viewModel { args -> InternetArchiveLoginViewModel(get(), args.get()) }
}
