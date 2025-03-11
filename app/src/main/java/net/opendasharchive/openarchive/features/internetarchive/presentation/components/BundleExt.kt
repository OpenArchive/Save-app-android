package net.opendasharchive.openarchive.features.internetarchive.presentation.components

import android.os.Bundle
import androidx.core.os.bundleOf
import net.opendasharchive.openarchive.db.Space

@Deprecated("only for use with fragments and activities")
private const val ARG_VAL_NEW_SPACE = -1L

@Deprecated("only for use with fragments and activities")
private const val ARG_SPACE = "space_id"

@Deprecated("only for use with fragments and activities")
enum class IAResult(
    val value: String
) {
    Saved("ia_fragment_resp_saved"), Deleted("ia_fragment_resp_deleted"), Cancelled("ia_fragment_resp_cancel"),
}

@Deprecated("only for use with fragments and activities")
fun bundleWithSpaceId(spaceId: Long) = bundleOf(ARG_SPACE to spaceId)

@Deprecated("only for use with fragments and activities")
fun bundleWithNewSpace() = bundleOf(ARG_SPACE to ARG_VAL_NEW_SPACE)

@Deprecated("only for use with fragments and activities")
fun Bundle?.getSpace(type: Space.Type): Pair<Space, Boolean> {
    val mSpaceId = this?.getLong(ARG_SPACE) ?: ARG_VAL_NEW_SPACE

    val isNewSpace = ARG_VAL_NEW_SPACE == mSpaceId

    return if (isNewSpace) {
        Pair(Space(type), true)
    } else {
        Space.get(mSpaceId)?.let { Pair(it, false) } ?: Pair(Space(type), true)
    }
}
