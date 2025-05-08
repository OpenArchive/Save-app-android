package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.tor.TorRepository
import net.opendasharchive.openarchive.services.tor.torModule
import org.koin.dsl.module

val coreModule = module {
    includes(torModule)
}
