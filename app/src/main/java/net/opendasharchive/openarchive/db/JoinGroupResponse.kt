package net.opendasharchive.openarchive.db

import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupResponse(
    val group: SnowbirdGroup
): SerializableMarker