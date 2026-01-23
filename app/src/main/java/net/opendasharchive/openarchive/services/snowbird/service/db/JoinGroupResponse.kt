package net.opendasharchive.openarchive.services.snowbird.service.db

import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupResponse(
    val group: SnowbirdGroup
): SerializableMarker