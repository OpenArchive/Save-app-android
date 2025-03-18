package net.opendasharchive.openarchive.db

import kotlinx.serialization.Serializable

@Serializable
data class RequestName(val name: String): SerializableMarker

@Serializable
data class MembershipRequest(val uri: String): SerializableMarker