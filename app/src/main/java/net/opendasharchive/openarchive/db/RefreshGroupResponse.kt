package net.opendasharchive.openarchive.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshGroupResponse(
    val status: String,
    @SerialName("repos")
    val refreshedRepos: List<RefreshedRepo>
) : SerializableMarker


@Serializable
data class RefreshedRepo(
    @SerialName("repo_id")
    val repoId: String,
    @SerialName("repo_hash")
    val hash: String? = null,
    val name: String,
    @SerialName("can_write")
    val canWrite: Boolean,
    @SerialName("all_files")
    val allFiles: List<String>,
    @SerialName("refreshed_files")
    val refreshedFiles: List<String>,
    @SerialName("error")
    val error: String? = null,
)

fun RefreshedRepo.toRepo(): SnowbirdRepo {
    return SnowbirdRepo(
        key = repoId,
        name = name,
        hash = hash,
        permissions = if (canWrite) "READ_WRITE" else "READ_ONLY",
        createdAt = null, // No createdAt in this response
    )
}