package net.opendasharchive.openarchive.db

import kotlinx.serialization.Serializable

@Serializable
sealed interface SerializableMarker

@Serializable
data object EmptyRequest : SerializableMarker