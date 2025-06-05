package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.features.main.UnixSocketClient
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.service.UnixSocketAPI
import org.koin.core.qualifier.named
import org.koin.dsl.module

val unixSocketModule = module {
    single {
        UnixSocketClient(context = get())
    }

    single<ISnowbirdAPI>(named("unixSocket")) { UnixSocketAPI(get(), get()) }
}