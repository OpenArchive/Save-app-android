package net.opendasharchive.openarchive.services

import org.koin.dsl.module

internal val servicesModule = module {

    factory { SaveClient(get()) }
}