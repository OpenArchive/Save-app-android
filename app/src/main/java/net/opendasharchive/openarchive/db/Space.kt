package net.opendasharchive.openarchive.db

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.github.abdularis.civ.AvatarImageView
import com.orm.SugarRecord
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.gdrive.GDriveConduit
import net.opendasharchive.openarchive.services.internetarchive.IaConduit
import net.opendasharchive.openarchive.util.DrawableUtil
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/**
 * Space - Account to connect to.
 *
 * @property type
 * @property name   Server given name
 * @property username   username for server
 * @property displayname    not in use
 * @property password   password for login
 * @property host   server url
 * @property metaData
 * @property licenseUrl
 * @constructor Create empty Space
 */
data class Space(
    var type: Int = 0,
    var name: String = "",
    var username: String = "",
    var displayname: String = "",
    var password: String = "",
    var host: String = "",
    var metaData: String = "",
    private var licenseUrl: String? = null,
    // private var chunking: Boolean? = null
) : SugarRecord() {

    constructor(type: Type) : this() {
        tType = type

        when (type) {
            Type.WEBDAV -> {}
            Type.INTERNET_ARCHIVE -> {
                name = IaConduit.NAME
                host = IaConduit.ARCHIVE_API_ENDPOINT
            }

            Type.GDRIVE -> {
                name = GDriveConduit.NAME
            }

            Type.RAVEN -> "Raven"
        }
    }

    enum class Type(val id: Int, val friendlyName: String) {
        WEBDAV(0, "Private Server"),
        INTERNET_ARCHIVE(1, IaConduit.NAME),
        GDRIVE(4, GDriveConduit.NAME),
        RAVEN(5, "DWeb Service"),
    }

    enum class IconStyle {
        SOLID, OUTLINE
    }

    companion object {
        fun getAll(): Iterator<Space> {
            return findAll(Space::class.java)
        }

        fun get(type: Type, host: String? = null, username: String? = null): List<Space> {
            var whereClause = "type = ?"
            val whereArgs = mutableListOf(type.id.toString())

            if (!host.isNullOrEmpty()) {
                whereClause = "$whereClause AND host = ?"
                whereArgs.add(host)
            }

            if (!username.isNullOrEmpty()) {
                whereClause = "$whereClause AND username = ?"
                whereArgs.add(username)
            }

            return find(
                Space::class.java, whereClause, whereArgs.toTypedArray(),
                null, null, null
            )
        }

        fun has(type: Type, host: String? = null, username: String? = null): Boolean {
            return get(type, host, username).isNotEmpty()
        }

        var current: Space?
            get() {
                AppLogger.i("getting current space....")
                return get(Prefs.currentSpaceId) ?: first(Space::class.java)
            }
            set(value) {
                AppLogger.i("setting current space... ${value?.displayname}")
                Prefs.currentSpaceId = value?.id ?: -1
            }

        fun get(id: Long): Space? {
            return findById(Space::class.java, id)
        }

        fun navigate(activity: AppCompatActivity) {
            if (getAll().hasNext()) {
                activity.finish()
            } else {
                activity.finishAffinity()
                activity.startActivity(Intent(activity, SpaceSetupActivity::class.java))
            }
        }
    }

    val friendlyName: String
        get() {
            if (name.isNotBlank()) {
                return name
            }

            return hostUrl?.host ?: name
        }

    val initial: String
        get() = (friendlyName.firstOrNull() ?: 'X').uppercase(Locale.getDefault())

    val hostUrl: HttpUrl?
        get() = host.toHttpUrlOrNull()

    var tType: Type
        get() = Type.entries.first { it.id == type }
        set(value) {
            type = (value ?: Type.WEBDAV).id
        }

    var license: String?
        get() = this.licenseUrl
        set(value) {
            licenseUrl = value

            for (project in projects) {
                project.licenseUrl = licenseUrl
                project.save()
            }
        }

//    var useChunking: Boolean
//        // Fallback to old preferences setting.
//        get() = chunking ?: Prefs.useNextcloudChunking
//        set(value) {
//            chunking = value
//        }

    val projects: List<Project>
        get() = find(
            Project::class.java,
            "space_id = ? AND NOT archived",
            arrayOf(id.toString()),
            null,
            "id DESC",
            null
        )

    val archivedProjects: List<Project>
        get() = find(
            Project::class.java,
            "space_id = ? AND archived",
            arrayOf(id.toString()),
            null,
            "id DESC",
            null
        )

    fun hasProject(description: String): Boolean {
        // Cannot use `count` from Kotlin due to strange <T> in method signature.
        return find(
            Project::class.java,
            "space_id = ? AND description = ?",
            id.toString(),
            description
        ).size > 0
    }

    fun getAvatar(context: Context, style: IconStyle = IconStyle.SOLID): Drawable? {


        return when (tType) {
            Type.WEBDAV -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_private_server
            ) // ?.tint(color)

            Type.INTERNET_ARCHIVE -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_internet_archive
            ) // ?.tint(color)

            Type.GDRIVE -> ContextCompat.getDrawable(
                context,
                R.drawable.logo_gdrive_outline
            ) // ?.tint(color)

            Type.RAVEN -> ContextCompat.getDrawable(context, R.drawable.snowbird) // ?.tint(color)

            else -> {
                val color = ContextCompat.getColor(context, R.color.colorOnBackground)
                BitmapDrawable(
                    context.resources,
                    DrawableUtil.createCircularTextDrawable(initial, color)
                )
            }

        }
    }

    @Composable
    fun getAvatar(): Painter {

        return when (tType) {
            Type.WEBDAV -> painterResource(R.drawable.ic_space_private_server)

            Type.INTERNET_ARCHIVE -> painterResource(R.drawable.ic_space_interent_archive)

            Type.GDRIVE -> painterResource(R.drawable.logo_gdrive_outline)

            Type.RAVEN -> painterResource(R.drawable.ic_space_dweb)
            null -> {
                val context = LocalContext.current
                val color = ContextCompat.getColor(context, R.color.colorOnBackground)
                val bitmap = DrawableUtil.createCircularTextDrawable(initial, color)
                val imageBitmap = bitmap.asImageBitmap()
                BitmapPainter(imageBitmap)
            }
        }
    }

    fun setAvatar(view: ImageView) {
        when (tType) {
            Type.INTERNET_ARCHIVE -> {
                if (view is AvatarImageView) {
                    view.state = AvatarImageView.SHOW_IMAGE
                }

                view.setImageDrawable(getAvatar(view.context))
            }

            else -> {
                if (view is AvatarImageView) {
                    view.state = AvatarImageView.SHOW_INITIAL
                    view.setText(initial)
                    view.avatarBackgroundColor =
                        ContextCompat.getColor(view.context, R.color.colorPrimary)
                } else {
                    view.setImageDrawable(getAvatar(view.context))
                }
            }
        }
    }

    override fun delete(): Boolean {
        projects.forEach {
            it.delete()
        }

        return super.delete()
    }
}