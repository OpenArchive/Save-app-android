package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.servicesModule
import org.koin.dsl.module

val coreModule = module {
    includes(servicesModule)
}
