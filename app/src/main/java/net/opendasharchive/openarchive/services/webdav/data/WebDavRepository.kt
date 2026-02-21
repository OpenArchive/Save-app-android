package net.opendasharchive.openarchive.services.webdav.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.toKotlinLocalDateTime
import java.io.IOException

class WebDavRepository(
    private val context: Context,
    private val spaceRepository: SpaceRepository
) {
    @Throws(IOException::class)
    suspend fun getFolders(vault: Vault): List<Folder> = withContext(Dispatchers.IO) {
        val auth = spaceRepository.getVaultAuth(vault.id)
            ?: throw IOException("Credentials unavailable for selected server")
        val root = vault.hostUrl?.encodedPath

        SaveClient.getSardine(context, auth.username, auth.secret).list(vault.host)?.mapNotNull {
            if (it?.isDirectory == true && it.path != root) {
                Folder(it.name, it.modified?.toKotlinLocalDateTime() ?: DateUtils.nowDateTime)
            } else {
                null
            }
        } ?: emptyList()
    }
}
