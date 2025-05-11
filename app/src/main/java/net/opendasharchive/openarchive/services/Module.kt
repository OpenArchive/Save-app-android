package net.opendasharchive.openarchive.services

import net.opendasharchive.openarchive.services.gdrive.gdriveModule
import net.opendasharchive.openarchive.services.tor.torModule
import org.koin.dsl.module

internal val servicesModule = module {

    single { SaveClient(get()) }

    includes(torModule, gdriveModule)
}