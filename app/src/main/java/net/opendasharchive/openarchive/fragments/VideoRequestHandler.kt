package net.opendasharchive.openarchive.fragments

import android.content.Context
import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException
import java.lang.Exception
import androidx.core.net.toUri

class VideoRequestHandler(private val mContext: Context) : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        val scheme = data.uri.scheme
        return SCHEME_VIDEO == scheme
    }

    @Throws(IOException::class)
    override fun load(data: Request, arg1: Int): Result? {
        val bm: Bitmap?
        try {
            bm = retrieveVideoFrameFromVideo(mContext, data.uri.toString().substring(6).toUri())
            if (bm != null) return Result(bm, Picasso.LoadedFrom.DISK)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
        return null
    }

    companion object {
        const val SCHEME_VIDEO = "video"
        @Throws(Throwable::class)
        fun retrieveVideoFrameFromVideo(context: Context?, videoPath: Uri?): Bitmap? {
            val bitmap: Bitmap?
            var mediaMetadataRetriever: MediaMetadataRetriever? = null
            try {
                mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(context, videoPath)
                bitmap =
                    mediaMetadataRetriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST)
            } catch (e: Exception) {
                throw Throwable("Exception in retrieveVideoFrameFromVideo(String videoPath)" + e.message)
            } finally {
                mediaMetadataRetriever?.release()
            }
            return bitmap
        }
    }
}