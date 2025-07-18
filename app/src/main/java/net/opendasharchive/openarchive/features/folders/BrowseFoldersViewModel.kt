package net.opendasharchive.openarchive.features.folders

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.gdrive.GDriveConduit
import timber.log.Timber
import java.io.IOException
import java.util.Date



data class Folder(val name: String, val modified: Date)

class BrowseFoldersViewModel(private val context: Context) : ViewModel() {

    private val mFolders = MutableLiveData<List<Folder>>()

    val folders: LiveData<List<Folder>>
        get() = mFolders

    val progressBarFlag = MutableLiveData(false)

    fun getFiles(space: Space) {
        viewModelScope.launch {
            progressBarFlag.value = true

            try {
                val value = withContext(Dispatchers.IO) {
                    when (space.tType) {
                        Space.Type.WEBDAV -> getWebDavFolders(context, space)

                        Space.Type.GDRIVE -> getGDriveFolders(context, space)

                        else -> emptyList()
                    }
                }

                mFolders.value = value.filter { !space.hasProject(it.name) }
                progressBarFlag.value = false
            }
            // Dropbox might throw all sorts of non-IOExceptions.
            catch (e: Throwable) {
                progressBarFlag.value = false
                mFolders.value = arrayListOf()

                Timber.e(e)
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun getWebDavFolders(context: Context, space: Space): List<Folder> {
        val root = space.hostUrl?.encodedPath

        return SaveClient.getSardine(context, space).list(space.host)?.mapNotNull {
            if (it?.isDirectory == true && it.path != root) {
                Folder(it.name, it.modified ?: Date())
            }
            else {
                null
            }
        } ?: emptyList()
    }

    private fun getGDriveFolders(context: Context, space: Space): List<Folder> {
        return GDriveConduit.listFoldersInRoot(GDriveConduit.getDrive(context))
    }
}