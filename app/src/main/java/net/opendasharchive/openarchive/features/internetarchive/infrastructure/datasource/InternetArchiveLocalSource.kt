package net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive

class InternetArchiveLocalSource {
    // TODO: just use a memory cache for demo, will need to store in DB
    //       the database should be SQLCipher (https://www.zetetic.net/sqlcipher/)
    //       as we are storing access keys.  Sugar record does not support sql cipher
    //       so planning a migration using local data sources.
    private val cache = MutableStateFlow<InternetArchive?>(null)

    fun set(value: InternetArchive) =  cache.update { value }

    fun get() = cache.value

    fun subscribe() = cache.filterNotNull()
}