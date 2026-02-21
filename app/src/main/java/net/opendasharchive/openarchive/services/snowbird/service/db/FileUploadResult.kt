package net.opendasharchive.openarchive.services.snowbird.service.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResult (
    var name: String,
    @SerialName("updated_collection_hash") var updatedCollectionHash: String,
    @SerialName("file_hash") var fileHash: String? = null
) : SerializableMarker
