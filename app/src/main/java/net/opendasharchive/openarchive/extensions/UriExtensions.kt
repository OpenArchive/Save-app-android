package net.opendasharchive.openarchive.extensions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

fun Uri.createInputStream(applicationContext: Context): InputStream? {
    return applicationContext.contentResolver.openInputStream(this)
}

fun Uri.getFilename(applicationContext: Context): String? {
        var result: String? = null
        if (this.scheme == "content") {
            val cr = applicationContext.contentResolver.query(this, null, null, null, null)
            cr.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = this.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
}