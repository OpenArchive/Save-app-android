package net.opendasharchive.openarchive.db

import android.content.Context
import android.net.Uri
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.orm.SugarRecord
import net.opendasharchive.openarchive.util.extensions.DecryptedFileProvider
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date

data class Media(
    var encryptedFilePath: String = "",

    @Expose
    @SerializedName("contentType")
    var mimeType: String = "",

    @Expose
    @SerializedName("dateCreated")
    var createDate: Date? = null,

    var updateDate: Date? = null,
    var uploadDate: Date? = null,
    var serverUrl: String = "",

    @Expose
    @SerializedName("originalFileName")
    var title: String = "",

    @Expose
    var description: String = "",

    @Expose
    var author: String = "",

    @Expose
    var location: String = "",

    @Expose
    var tags: String = "",

    @Expose
    @SerializedName("usage")
    var licenseUrl: String? = null,

    var mediaHash: ByteArray = byteArrayOf(),

    @Expose
    @SerializedName(value = "hash")
    var mediaHashString: String = "",

    var status: Int = 0,
    var statusMessage: String = "",
    var projectId: Long = 0,
    var collectionId: Long = 0,

    @Expose
    var contentLength: Long = 0,

    var progress: Long = 0,
    var flag: Boolean = false,
    var priority: Int = 0,
    var selected: Boolean = false
) : SugarRecord() {

    enum class Status(val id: Int) {
        New(0),
        Local(1),
        Queued(2),

        @Deprecated("Actually unused.", ReplaceWith("Uploaded"))
        Published(3),
        Uploading(4),
        Uploaded(5),

        @Deprecated("Save does not do deletion.")
        DeleteRemote(7),
        Error(9),
    }

    companion object {
        const val ORDER_PRIORITY = "priority DESC"
        const val ORDER_CREATED = "create_date DESC"

        fun getByStatus(statuses: List<Status>, order: String? = null): List<Media> {
            return find(
                Media::class.java,
                statuses.joinToString(" OR ") { "status = ?" },
                statuses.map { it.id.toString() }.toTypedArray(),
                null, order, null
            )
        }

        fun get(mediaId: Long?): Media? {
            @Suppress("NAME_SHADOWING")
            val mediaId = mediaId ?: return null
            return findById(Media::class.java, mediaId)
        }
    }

    val formattedCreateDate: String
        get() {
            return createDate?.let {
                // You can replace this with a fixed format if needed:
                // SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(it)
            } ?: ""
        }

    var sStatus: Status
        get() = Status.entries.firstOrNull { it.id == status } ?: Status.New
        set(value) {
            status = value.id
        }

    /**
     * Returns a decrypted content URI for this Media.
     */
    fun getFileUri(context: Context): Uri {
        return DecryptedFileProvider.getUriForFile(context, File(encryptedFilePath))
    }

    /**
     * Returns an InputStream that provides decrypted file contents.
     */
    fun fileInputStream(context: Context): InputStream? {
        return context.contentResolver.openInputStream(getFileUri(context))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Media

        if (encryptedFilePath != other.encryptedFilePath) return false
        if (mimeType != other.mimeType) return false
        if (createDate != other.createDate) return false
        if (updateDate != other.updateDate) return false
        if (uploadDate != other.uploadDate) return false
        if (serverUrl != other.serverUrl) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (author != other.author) return false
        if (location != other.location) return false
        if (tags != other.tags) return false
        if (licenseUrl != other.licenseUrl) return false
        if (!mediaHash.contentEquals(other.mediaHash)) return false
        if (mediaHashString != other.mediaHashString) return false
        if (status != other.status) return false
        if (statusMessage != other.statusMessage) return false
        if (projectId != other.projectId) return false
        if (collectionId != other.collectionId) return false
        if (contentLength != other.contentLength) return false
        if (progress != other.progress) return false
        if (flag != other.flag) return false
        if (priority != other.priority) return false
        if (selected != other.selected) return false
        if (formattedCreateDate != other.formattedCreateDate) return false
        if (sStatus != other.sStatus) return false
        if (collection != other.collection) return false
        if (project != other.project) return false
        if (space != other.space) return false
        if (isUploading != other.isUploading) return false
        if (tagSet != other.tagSet) return false
        if (uploadPercentage != other.uploadPercentage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedFilePath.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (createDate?.hashCode() ?: 0)
        result = 31 * result + (updateDate?.hashCode() ?: 0)
        result = 31 * result + (uploadDate?.hashCode() ?: 0)
        result = 31 * result + serverUrl.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (licenseUrl?.hashCode() ?: 0)
        result = 31 * result + mediaHash.contentHashCode()
        result = 31 * result + mediaHashString.hashCode()
        result = 31 * result + status
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + projectId.hashCode()
        result = 31 * result + collectionId.hashCode()
        result = 31 * result + contentLength.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + flag.hashCode()
        result = 31 * result + priority
        result = 31 * result + selected.hashCode()
        result = 31 * result + formattedCreateDate.hashCode()
        result = 31 * result + sStatus.hashCode()
        result = 31 * result + (collection?.hashCode() ?: 0)
        result = 31 * result + (project?.hashCode() ?: 0)
        result = 31 * result + (space?.hashCode() ?: 0)
        result = 31 * result + isUploading.hashCode()
        result = 31 * result + tagSet.hashCode()
        result = 31 * result + (uploadPercentage ?: 0)
        return result
    }

    val collection: Collection?
        get() = findById(Collection::class.java, collectionId)

    val project: Project?
        get() = findById(Project::class.java, projectId)

    val space: Space?
        get() = project?.space

    /**
     * Indicates whether the media is currently uploading.
     * (Note: Currently includes the Error state; adjust if you wish to exclude it.)
     */
    val isUploading: Boolean
        get() = status == Status.Queued.id ||
                status == Status.Uploading.id ||
                status == Status.Error.id

    /**
     * Splits the tags string into a set, filtering out any empty strings.
     */
    var tagSet: MutableSet<String>
        get() = tags.split("\\p{Punct}|[ \\t]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        set(value) {
            tags = value.joinToString(";")
        }

    @Transient
    var uploadPercentage: Int? = null
}