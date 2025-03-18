package net.opendasharchive.openarchive.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResult (
    var name: String,
    @SerialName("updated_collection_hash") var updatedCollectionHash: String
) : SerializableMarker