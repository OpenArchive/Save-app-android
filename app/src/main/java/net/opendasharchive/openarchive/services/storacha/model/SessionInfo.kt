package net.opendasharchive.openarchive.services.storacha.model

data class SessionInfo(
    val sessionId: String,
    val createdAt: String,
    val lastActive: String,
    val isActive: Boolean,
)
