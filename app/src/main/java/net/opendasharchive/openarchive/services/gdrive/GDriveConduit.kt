@file:Suppress("DEPRECATION")

package net.opendasharchive.openarchive.services.gdrive

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.util.Prefs
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.params.ConnPerRouteBean
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.Date

/**
 * This class contains all communication with / integration of Google Drive
 *
 * The only actually working documentation I could find about Googles Android GDrive API is this:
 * https://stackoverflow.com/questions/56949872/
 * There's also this official documentation by Google for accessing GDrive, however it was pretty
 * useless to me, since it doesn't explain what's going on at all. (I also couldn't get it to run
 * in a reasonable amount of time):
 * https://github.com/googleworkspace/android-samples/tree/master/drive/deprecation
 * The official documentation doesn't mention Android and the Java Sample is only useful for
 * integrating GDrive into backends. However it's still helpful for figuring building queries:
 * https://developers.google.com/drive/api/guides/about-sdk
 * Another important resource is this official guide on authenticating an Android app with Google:
 * https://developers.google.com/identity/sign-in/android/start-integrating
 */
class GDriveConduit(media: Media, context: Context) : Conduit(media, context) {

    private var mDrive: Drive = getDrive(mContext)

    companion object {
        const val NAME = "Google Drive"
        var SCOPES =
            arrayOf(Scope(DriveScopes.DRIVE_FILE), Scope(Scopes.EMAIL))

        fun permissionsGranted(context: Context): Boolean {
            Timber.v("GDriveConduit.permissionGranted()")
            return GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(context),
                *SCOPES
            )
        }

        fun getDrive(context: Context): Drive {
            val credential =
                GoogleAccountCredential.usingOAuth2(
                    context,
                    setOf(DriveScopes.DRIVE_FILE, Scopes.EMAIL)
                )
            credential.selectedAccount = GoogleSignIn.getLastSignedInAccount(context)?.account

            // in case we need to debug authentication:
            // Timber.v("GDriveConduit.getDrive(): credential $credential")
            // Timber.v("GDriveConduit.getDrive(): credential.selectedAccount ${credential.selectedAccount}")
            // Timber.v("GDriveConduit.getDrive(): credential.selectedAccount.name ${credential.selectedAccount?.name}")

            val transport: HttpTransport = if (Prefs.useTor) {
                // initialization code copied from: ApacheHttpTransport.newDefaultHttpParams()
                // This is the simplest solution I could come up with for actually sending traffic
                // to GDrive through Tor. Note that all calls to deprecated functions are copied
                // from the only known to work version of GDrive API.
                val params = BasicHttpParams()
                HttpConnectionParams.setStaleCheckingEnabled(params, false)
                HttpConnectionParams.setSocketBufferSize(params, 8192)
                ConnManagerParams.setMaxTotalConnections(params, 200)
                ConnManagerParams.setMaxConnectionsPerRoute(params, ConnPerRouteBean(20))
                val registry = SchemeRegistry()
                registry.register(Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
                registry.register(Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
                val connectionManager: ClientConnectionManager =
                    ThreadSafeClientConnManager(params, registry)
                val defaultHttpClient = DefaultHttpClient(connectionManager, params)
                defaultHttpClient.httpRequestRetryHandler = DefaultHttpRequestRetryHandler(0, false)
                val proxySelector = object : ProxySelector() {
                    override fun select(uri: URI?): MutableList<Proxy> {
                        return mutableListOf(
                            // tried SOCKS here, but in my tests when specifying SOCKS, the uploads
                            // seamed to bypass proxy settings altogether and connect directly instead
                            Proxy(
                                Proxy.Type.HTTP,
                                InetSocketAddress(
                                    OrbotHelper.DEFAULT_PROXY_HOST,
                                    OrbotHelper.DEFAULT_PROXY_HTTP_PORT
                                )
                            )
                        )
                    }

                    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                        Timber.e("proxy connection Failed ($uri, $sa)", ioe)
                    }
                }
                defaultHttpClient.routePlanner = ProxySelectorRoutePlanner(
                    registry,
                    proxySelector
                )

                ApacheHttpTransport(defaultHttpClient)
            } else {
                AndroidHttp.newCompatibleTransport()
            }

            return Drive.Builder(transport, GsonFactory(), credential)
                .setApplicationName(ContextCompat.getString(context, R.string.app_name)).build()
        }

        private fun createFolder(gdrive: Drive, folderName: String, parent: File?): File {
            val parentId: String = parent?.id ?: "root"
            val folders =
                gdrive.files().list().setPageSize(1)
                    .setQ("mimeType='application/vnd.google-apps.folder' and name = '$folderName' and trashed = false and '$parentId' in parents")
                    .setFields("files(id, name)").execute()

            if (folders.files.isNotEmpty()) {
                // folder exists, return it now
                return folders.files.first()
            }

            // create new folder
            val folderMeta = File()
            folderMeta.name = folderName
            folderMeta.parents = listOf(parentId)
            folderMeta.mimeType = "application/vnd.google-apps.folder"

            // return newly created folders
            return gdrive.files().create(folderMeta).setFields("id").execute()
        }

        fun createFolders(mDrive: Drive, destinationPath: List<String>): File {
            var parentFolder: File? = null
            for (pathElement in destinationPath) {
                parentFolder = createFolder(mDrive, pathElement, parentFolder)
            }
            if (parentFolder == null) {
                throw Exception("could not create folders $destinationPath")
            }
            return parentFolder
        }

        fun listFoldersInRoot(gdrive: Drive): List<Folder> {
            val result = ArrayList<Folder>()
            try {
                var pageToken: String? = null
                do {
                    val folders =
                        gdrive.files().list().setPageSize(1000).setPageToken(pageToken)
                            .setQ("mimeType='application/vnd.google-apps.folder' and 'root' in parents and trashed = false")
                            .setFields("nextPageToken, files(id, name, createdTime)").execute()
                    for (f in folders.files) {
                        val date = Date(f.createdTime.value)
                        result.add(Folder(f.name, date))
                    }
                    pageToken = folders.nextPageToken
                } while (pageToken != null)
            } catch (e: java.lang.IllegalArgumentException) {
                Timber.e(e)
            }
            return result
        }
    }

    override suspend fun upload(): Boolean {
        val destinationPath = getPath() ?: return false
        val destinationFileName = getUploadFileName(mMedia)
        sanitize()

        try {
            val folder = createFolders(mDrive, destinationPath)
            uploadMetadata(folder, destinationFileName)
            if (mCancelled) throw Exception("Cancelled")
            uploadFile(mMedia.file, folder, destinationFileName)
        } catch (e: Exception) {
            jobFailed(e)
            return false
        }

        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        throw NotImplementedError("the createFolder calls defined in Conduit don't map to GDrive API. use GDriveConduit.createFolder instead")
    }

    private fun uploadMetadata(parent: File, fileName: String) {
        val metadataFileName = "$fileName.meta.json"

        if (mCancelled) throw java.lang.Exception("Cancelled")

        uploadFile(getMetadata().byteInputStream(), parent, metadataFileName)

        for (file in getProof()) {
            if (mCancelled) throw java.lang.Exception("Cancelled")

            uploadFile(file, parent, file.name)
        }
    }

    private fun uploadFile(
        sourceFile: java.io.File,
        parentFolder: File,
        targetFileName: String,
    ) {
        uploadFile(sourceFile.inputStream(), parentFolder, targetFileName)
    }

    private fun uploadFile(
        inputStream: InputStream,
        parentFolder: File,
        targetFileName: String,
    ) {
        try {
            val fMeta = File()
            fMeta.name = targetFileName
            fMeta.parents = listOf(parentFolder.id)
            val request =
                mDrive.files().create(fMeta, InputStreamContent(null, inputStream))
            request.mediaHttpUploader.isDirectUploadEnabled = false
            request.mediaHttpUploader.chunkSize =
                262144  // magic minimum chunk-size number (smaller number will cause exception)
            request.mediaHttpUploader.setProgressListener {
                if (it.uploadState == MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS) {
                    jobProgress(it.numBytesUploaded)
                }
            }
            val response = request.execute()
        } catch (e: Exception) {
            Timber.e("gdrive upload of '$targetFileName' failed", e)
        }
    }
}