package net.opendasharchive.openarchive.analytics.api

/**
 * Sealed interface representing all analytics events in the application
 * GDPR-Compliant: Contains NO PII (personally identifiable information)
 *
 * Modern implementation using sealed interface for better composition
 */
sealed interface AnalyticsEvent {
    val category: String
    val action: String
    val label: String?
    val value: Double?
    val properties: Map<String, Any>

    // ==================== APP LIFECYCLE ====================

    data class AppOpened(
        val isFirstLaunch: Boolean = false,
        val appVersion: String,
    ) : AnalyticsEvent {
        override val category = "app"
        override val action = "opened"
        override val label: String? = null
        override val value: Double? = null
        override val properties = mapOf(
            "is_first_launch" to isFirstLaunch,
            "app_version" to appVersion,
        )
    }

    data class AppClosed(
        val sessionDurationSeconds: Long,
    ) : AnalyticsEvent {
        override val category = "app"
        override val action = "closed"
        override val label: String? = null
        override val value = sessionDurationSeconds.toDouble()
        override val properties: Map<String, Any> = emptyMap()
    }

    data object AppBackgrounded : AnalyticsEvent {
        override val category = "app"
        override val action = "backgrounded"
        override val label: String? = null
        override val value: Double? = null
        override val properties: Map<String, Any> = emptyMap()
    }

    data object AppForegrounded : AnalyticsEvent {
        override val category = "app"
        override val action = "foregrounded"
        override val label: String? = null
        override val value: Double? = null
        override val properties: Map<String, Any> = emptyMap()
    }

    // ==================== SCREEN TRACKING ====================

    data class ScreenViewed(
        val screenName: String,
        val timeSpentSeconds: Long? = null,
        val previousScreen: String? = null,
    ) : AnalyticsEvent {
        override val category = "screen"
        override val action = "viewed"
        override val label = screenName
        override val value = timeSpentSeconds?.toDouble()
        override val properties = mapOf(
            "screen_name" to screenName,
            "previous_screen" to (previousScreen ?: "none"),
        )
    }

    data class NavigationAction(
        val fromScreen: String,
        val toScreen: String,
        val trigger: String? = null,
    ) : AnalyticsEvent {
        override val category = "navigation"
        override val action = "screen_change"
        override val label = "$fromScreen -> $toScreen"
        override val value: Double? = null
        override val properties = mapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen,
            "trigger" to (trigger ?: "unknown"),
        )
    }

    // ==================== BACKEND USAGE ====================

    data class BackendConfigured(
        val backendType: String, // "Internet Archive", "Private Server", "DWeb Service", "Storacha"
        val isNew: Boolean = true,
    ) : AnalyticsEvent {
        override val category = "backend"
        override val action = if (isNew) "configured" else "updated"
        override val label = backendType
        override val value: Double? = null
        override val properties = mapOf(
            "backend_type" to backendType,
            "is_new" to isNew,
        )
    }

    data class BackendRemoved(
        val backendType: String,
        val reason: String? = null,
    ) : AnalyticsEvent {
        override val category = "backend"
        override val action = "removed"
        override val label = backendType
        override val value: Double? = null
        override val properties = mapOf(
            "backend_type" to backendType,
            "reason" to (reason ?: "unknown"),
        )
    }

    // ==================== UPLOAD METRICS ====================

    data class UploadStarted(
        val backendType: String,
        val fileType: String, // "image", "video", "document", "other"
        val fileSizeKB: Long,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "started"
        override val label = backendType
        override val value: Double? = null
        override val properties = mapOf(
            "backend_type" to backendType,
            "file_type" to fileType,
            "file_size_kb" to fileSizeKB,
            "file_size_category" to getFileSizeCategory(fileSizeKB),
        )

        companion object {
            internal fun getFileSizeCategory(sizeKB: Long): String =
                when {
                    sizeKB < 100 -> "tiny"      // < 100KB
                    sizeKB < 1024 -> "small"    // < 1MB
                    sizeKB < 10240 -> "medium"  // < 10MB
                    sizeKB < 102400 -> "large"  // < 100MB
                    else -> "very_large"        // >= 100MB
                }
        }
    }

    data class UploadCompleted(
        val backendType: String,
        val fileType: String,
        val fileSizeKB: Long,
        val durationSeconds: Long,
        val uploadSpeedKBps: Long? = null,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "completed"
        override val label = backendType
        override val value = durationSeconds.toDouble()
        override val properties = mapOf(
            "backend_type" to backendType,
            "file_type" to fileType,
            "file_size_kb" to fileSizeKB,
            "duration_seconds" to durationSeconds,
            "upload_speed_kbps" to (uploadSpeedKBps ?: 0),
            "file_size_category" to UploadStarted.getFileSizeCategory(fileSizeKB),
        )
    }

    data class UploadFailed(
        val backendType: String,
        val fileType: String,
        val errorCategory: String, // "network", "permission", "file_not_found", "storage", "unknown"
        val fileSizeKB: Long? = null,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "failed"
        override val label = backendType
        override val value: Double? = null
        override val properties = mapOf(
            "backend_type" to backendType,
            "file_type" to fileType,
            "error_category" to errorCategory,
            "file_size_kb" to (fileSizeKB ?: 0),
        )
    }

    // ==================== MEDIA ACTIONS ====================

    data class MediaCaptured(
        val mediaType: String, // "photo", "video"
        val source: String = "camera",
    ) : AnalyticsEvent {
        override val category = "media"
        override val action = "captured"
        override val label = mediaType
        override val value: Double? = null
        override val properties = mapOf(
            "media_type" to mediaType,
            "source" to source,
        )
    }

    data class MediaSelected(
        val count: Int,
        val source: String, // "gallery", "camera", "files"
        val mediaTypes: List<String> = emptyList(),
    ) : AnalyticsEvent {
        override val category = "media"
        override val action = "selected"
        override val label = source
        override val value = count.toDouble()
        override val properties = mapOf(
            "count" to count,
            "source" to source,
            "has_images" to mediaTypes.contains("image"),
            "has_videos" to mediaTypes.contains("video"),
            "has_documents" to mediaTypes.contains("document"),
        )
    }

    data class MediaDeleted(
        val count: Int,
    ) : AnalyticsEvent {
        override val category = "media"
        override val action = "deleted"
        override val label: String? = null
        override val value = count.toDouble()
        override val properties = mapOf("count" to count)
    }

    // ==================== FEATURE USAGE ====================

    data class FeatureToggled(
        val featureName: String, // "proofmode", "tor", "dark_mode", "wifi_only_upload"
        val enabled: Boolean,
    ) : AnalyticsEvent {
        override val category = "feature"
        override val action = if (enabled) "enabled" else "disabled"
        override val label = featureName
        override val value: Double? = null
        override val properties = mapOf(
            "feature_name" to featureName,
            "enabled" to enabled,
        )
    }

    // ==================== ERROR TRACKING ====================

    data class ErrorOccurred(
        val errorCategory: String, // "network", "permission", "upload", "auth", "storage", "unknown"
        val screenName: String,
        val backendType: String? = null,
    ) : AnalyticsEvent {
        override val category = "error"
        override val action = errorCategory
        override val label = screenName
        override val value: Double? = null
        override val properties = mapOf(
            "error_category" to errorCategory,
            "screen_name" to screenName,
            "backend_type" to (backendType ?: "none"),
        )
    }

    // ==================== SESSION TRACKING ====================

    data class SessionStarted(
        val isFirstSession: Boolean = false,
        val sessionNumber: Int = 1,
    ) : AnalyticsEvent {
        override val category = "session"
        override val action = "started"
        override val label: String? = null
        override val value = sessionNumber.toDouble()
        override val properties = mapOf(
            "is_first_session" to isFirstSession,
            "session_number" to sessionNumber,
        )
    }

    data class SessionEnded(
        val lastScreen: String,
        val durationSeconds: Long,
        val uploadsCompleted: Int = 0,
        val uploadsFailed: Int = 0,
    ) : AnalyticsEvent {
        override val category = "session"
        override val action = "ended"
        override val label = lastScreen
        override val value = durationSeconds.toDouble()
        override val properties = mapOf(
            "last_screen" to lastScreen,
            "duration_seconds" to durationSeconds,
            "uploads_completed" to uploadsCompleted,
            "uploads_failed" to uploadsFailed,
        )
    }

    // ==================== USAGE STATISTICS ====================

    data class DailyUsageStats(
        val totalUploads: Int,
        val totalUploadSizeMB: Long,
        val successRate: Float,
        val averageUploadTimeSec: Long,
        val mostUsedBackend: String,
    ) : AnalyticsEvent {
        override val category = "usage"
        override val action = "daily_stats"
        override val label: String? = null
        override val value: Double? = null
        override val properties = mapOf(
            "total_uploads" to totalUploads,
            "total_upload_size_mb" to totalUploadSizeMB,
            "success_rate" to successRate,
            "average_upload_time_sec" to averageUploadTimeSec,
            "most_used_backend" to mostUsedBackend,
        )
    }

    // ==================== UPLOAD SESSION TRACKING ====================

    data class UploadSessionStarted(
        val count: Int,
        val totalSizeMB: Long,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "session_started"
        override val label: String? = null
        override val value = count.toDouble()
        override val properties = mapOf(
            "count" to count,
            "total_size_mb" to totalSizeMB,
        )
    }

    data class UploadSessionCompleted(
        val count: Int,
        val successCount: Int,
        val failedCount: Int,
        val durationSeconds: Long,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "session_completed"
        override val label: String? = null
        override val value = durationSeconds.toDouble()
        override val properties = mapOf(
            "count" to count,
            "success_count" to successCount,
            "failed_count" to failedCount,
            "duration_seconds" to durationSeconds,
            "success_rate" to if (count > 0) (successCount.toFloat() / count * 100).toInt() else 0,
        )
    }

    data class UploadCancelled(
        val backendType: String,
        val fileType: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "cancelled"
        override val label = backendType
        override val value: Double? = null
        override val properties = mapOf(
            "backend_type" to backendType,
            "file_type" to fileType,
            "reason" to reason,
        )
    }

    data class UploadNetworkError(
        val reason: String, // "no_network", "wifi_required", "connection_lost"
    ) : AnalyticsEvent {
        override val category = "upload"
        override val action = "network_error"
        override val label = reason
        override val value: Double? = null
        override val properties = mapOf("reason" to reason)
    }

    // ==================== ENGAGEMENT TRACKING ====================

    data object ReviewPromptShown : AnalyticsEvent {
        override val category = "engagement"
        override val action = "review_prompt_shown"
        override val label: String? = null
        override val value: Double? = null
        override val properties: Map<String, Any> = emptyMap()
    }

    data object ReviewPromptCompleted : AnalyticsEvent {
        override val category = "engagement"
        override val action = "review_prompt_completed"
        override val label: String? = null
        override val value: Double? = null
        override val properties: Map<String, Any> = emptyMap()
    }

    data class ReviewPromptError(
        val errorCode: Int,
    ) : AnalyticsEvent {
        override val category = "engagement"
        override val action = "review_prompt_error"
        override val label: String? = null
        override val value = errorCode.toDouble()
        override val properties = mapOf("error_code" to errorCode)
    }
}
