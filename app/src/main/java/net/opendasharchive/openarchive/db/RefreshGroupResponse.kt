package net.opendasharchive.openarchive.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshGroupResponse(
    val status: String,
    @SerialName("refreshed_files")
    val refreshedFiles: List<String>
) : SerializableMarker