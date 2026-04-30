package net.opendasharchive.openarchive.services.internetarchive.data

import net.opendasharchive.openarchive.features.folders.Folder

class InternetArchiveRepository {
    // Currently IA doesn't require folder listing for target selection,
    // but the structure is standardized for future parity.
    suspend fun getFolders(): List<Folder> = emptyList()
}
