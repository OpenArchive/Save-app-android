package net.opendasharchive.openarchive.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import com.google.gson.GsonBuilder
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.gdrive.GDriveConduit
import net.opendasharchive.openarchive.services.internetarchive.IaConduit
import net.opendasharchive.openarchive.services.webdav.WebDavConduit
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.HttpUrl
import org.witness.proofmode.ProofMode
import org.witness.proofmode.crypto.HashUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class Conduit(
    protected val mMedia: Media,
    protected val mContext: Context
) {

    @SuppressLint("SimpleDateFormat")
    protected val mDateFormat = SimpleDateFormat(FOLDER_DATETIME_FORMAT)

    protected var mCancelled = false

    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     */
    @Throws(IOException::class)
    abstract suspend fun upload(): Boolean

    abstract suspend fun createFolder(url: String)

    open fun cancel() {
        mCancelled = true
    }

    fun getProof(): Array<out File> {
        if (!Prefs.useProofMode) return emptyArray()

        // Don't use geolocation and network information.
        Prefs.proofModeLocation = false
        Prefs.proofModeNetwork = false

        try {
            var hash = ProofMode.generateProof(
                mContext,
                Uri.parse(mMedia.originalFilePath),
                mMedia.mediaHashString
            )

            if (hash == null) {
                val proofHash = HashUtils.getSHA256FromFileContent(
                    mContext.contentResolver.openInputStream(mMedia.fileUri)
                )

                hash = ProofMode.generateProof(mContext, mMedia.fileUri, proofHash)
            }
            return ProofMode.getProofDir(mContext, mMedia.mediaHashString).listFiles() ?: emptyArray()
        } catch (exception: FileNotFoundException) {
            AppLogger.e(exception)

            return emptyArray()
        } catch (exception: SecurityException) {
            AppLogger.e(exception)

            return emptyArray()
        }
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     */
    fun jobSucceeded() {
        mMedia.progress = mMedia.contentLength
        mMedia.sStatus = Media.Status.Uploaded
        mMedia.save()
        AppLogger.i("media item ${mMedia.id} is uploaded and saved")
        BroadcastManager.postSuccess(
            context = mContext,
            collectionId = mMedia.collectionId,
            mediaId = mMedia.id
        )
    }

    fun jobFailed(exception: Throwable) {
        // If an upload was cancelled, ignore the error.
        if (mCancelled) {
            AppLogger.i("Upload cancelled", exception)
            return
        }

        mMedia.statusMessage =
            exception.localizedMessage ?: exception.message ?: exception.toString()
        mMedia.sStatus = Media.Status.Error
        mMedia.save()

        AppLogger.e(exception)

        BroadcastManager.postChange(
            context = mContext,
            collectionId = mMedia.collectionId,
            mediaId = mMedia.id
        )
    }

    private var lastReportedProgress: Int? = null

    fun jobProgress(uploadedBytes: Long) {
        mMedia.progress = uploadedBytes
        val progress = if (uploadedBytes > 0) (uploadedBytes.toFloat() / mMedia.contentLength * 100).toInt() else 0
        if (progress > (lastReportedProgress ?: 0) + 1) {
            lastReportedProgress = progress
            AppLogger.i("Media Item ${mMedia.id} progress: $progress/100")
            BroadcastManager.postProgress(
                context = mContext,
                collectionId = mMedia.collectionId,
                mediaId = mMedia.id,
                progress = progress,
            )
        }
    }

    /**
     * workaround to deal with some quirks in our data model?
     *
     * reads some values from mMedia and copies them to some other fields of mMedia
     */
    protected fun sanitize() {
        val length = mMedia.file.length()
        if (length > 0) mMedia.contentLength = length

        val tags = mMedia.tagSet

        if (mMedia.flag) {
            tags.add(getFlagText())
        } else {
            tags.remove(getFlagText())
        }

        mMedia.tagSet = tags

        // Update to the latest project license.
        mMedia.licenseUrl = mMedia.project?.licenseUrl
    }

    protected fun getPath(): List<String>? {
        val projectName = mMedia.project?.description ?: return null
        val collectionName =
            mDateFormat.format(mMedia.collection?.uploadDate ?: mMedia.createDate ?: Date())

        val path = mutableListOf(projectName, collectionName)

        if (mMedia.flag) {
            path.add(getFlagText())
        }

        return path
    }

    protected suspend fun createFolders(base: HttpUrl?, path: List<String>) {
        val tmp = mutableListOf<String>()

        for (segment in path) {
            tmp.add(segment)

            if (mCancelled) throw Exception("Cancelled")

            val url = construct(base, tmp)

            createFolder(url)
        }
    }

    /**
     * Constructs, either a full URL or a path from the given arguments, depending if a `base` is given.
     *
     * If there's only a path to be constructed, then the path will have a leading slash and will
     * not be escaped, as the Dropbox client likes it like that.
     *
     * If there's a full URL to be constructed, it *will* be escaped properly.
     */
    protected fun construct(base: HttpUrl?, path: List<String>, file: String? = null): String {
        val builder = base?.newBuilder() ?: HttpUrl.Builder().scheme("http").host("ignored")

        path.forEach { builder.addPathSegment(it) }

        if (!file.isNullOrBlank()) builder.addPathSegment(file)

        return if (base != null) {
            builder.toString()
        } else {
            "/${builder.build().pathSegments.joinToString("/")}"
        }
    }

    protected fun construct(path: List<String>, file: String? = null): String {
        return construct(null, path, file)
    }

    /**
     * Generate JSON encoded string of metadata corresponding Media currently
     * stored in `this.mMedia`.
     */
    protected fun getMetadata(): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create()

        return gson.toJson(this.mMedia, Media::class.java)
    }

    /**
     * Always use english, since this affects the target server, not the local device.
     */
    private fun getFlagText(): String {
        val conf = Configuration(mContext.resources.configuration)
        conf.setLocale(Locale.US)

        return mContext.createConfigurationContext(conf).getString(R.string.status_flagged)
    }

    companion object {
        const val FOLDER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'GMT'ZZZZZ"

        /**
         * 2 MByte
         */
        const val CHUNK_SIZE: Long = 2 * 1024 * 1024

        /**
         * 10 MByte
         */
        const val CHUNK_FILESIZE_THRESHOLD = 10 * 1024 * 1024

        fun get(media: Media, context: Context): Conduit? {
            return when (media.project?.space?.tType) {
                Space.Type.INTERNET_ARCHIVE -> IaConduit(media, context)

                Space.Type.WEBDAV -> WebDavConduit(media, context)

                Space.Type.GDRIVE -> GDriveConduit(media, context)

                else -> null
            }
        }

        fun getUploadFileName(media: Media, escapeTitle: Boolean = false): String {
            var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.mimeType)
            if (ext.isNullOrEmpty()) {
                ext = when {
                    media.mimeType.startsWith("image") -> "jpg"

                    media.mimeType.startsWith("video") -> "mp4"

                    media.mimeType.startsWith("audio") -> "m4a"

                    else -> "txt"
                }
            }

            var title = media.title

            if (title.isBlank()) title = media.mediaHashString

            if (escapeTitle) {
                title = UrlEscapers.urlPathSegmentEscaper().escape(title) ?: title
            }

            if (!title.endsWith(".$ext")) {
                return "$title.$ext"
            }

            return title
        }
    }
}