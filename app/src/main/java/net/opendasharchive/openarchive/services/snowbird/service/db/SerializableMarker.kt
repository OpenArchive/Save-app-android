package net.opendasharchive.openarchive.services.snowbird.service.db

import kotlinx.serialization.Serializable

@Serializable
sealed interface SerializableMarker

@Serializable
data object EmptyRequest : SerializableMarker