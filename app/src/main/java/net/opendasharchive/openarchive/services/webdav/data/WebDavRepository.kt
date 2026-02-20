package net.opendasharchive.openarchive.services.webdav.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.toKotlinLocalDateTime
import java.io.IOException

class WebDavRepository(
    private val context: Context
) {
    @Throws(IOException::class)
    suspend fun getFolders(vault: Vault): List<Folder> = withContext(Dispatchers.IO) {
        val root = vault.hostUrl?.encodedPath

        SaveClient.getSardine(context, vault.username, vault.password).list(vault.host)?.mapNotNull {
            if (it?.isDirectory == true && it.path != root) {
                Folder(it.name, it.modified?.toKotlinLocalDateTime() ?: DateUtils.nowDateTime)
            } else {
                null
            }
        } ?: emptyList()
    }
}
