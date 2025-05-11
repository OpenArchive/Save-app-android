package net.opendasharchive.openarchive.services.gdrive

data class GDriveFile(
    val id: String? = null,
    val name: String,
    val modifiedTime: String? = null,
    val parents: List<String> = emptyList()
)

data class GDriveFileList(
    val files: List<GDriveFile>,
    val nextPageToken: String? = null,
)