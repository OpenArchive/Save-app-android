package net.opendasharchive.openarchive.core.analytics

/**
 * Sealed class representing all analytics events in the application
 * GDPR-Compliant: Contains NO PII (personally identifiable information)
 */
sealed class AnalyticsEvent(
    val category: String,
    val action: String,
    val label: String? = null,
    val value: Double? = null,
    val properties: Map<String, Any> = emptyMap(),
) {
    // ==================== APP LIFECYCLE ====================

    data class AppOpened(
        val isFirstLaunch: Boolean = false,
        val appVersion: String,
    ) : AnalyticsEvent(
            category = "app",
            action = "opened",
            properties =
                mapOf(
                    "is_first_launch" to isFirstLaunch,
                    "app_version" to appVersion,
                ),
        )

    data class AppClosed(
        val sessionDurationSeconds: Long,
    ) : AnalyticsEvent(
            category = "app",
            action = "closed",
            value = sessionDurationSeconds.toDouble(),
        )

    class AppBackgrounded :
        AnalyticsEvent(
            category = "app",
            action = "backgrounded",
        )

    class AppForegrounded :
        AnalyticsEvent(
            category = "app",
            action = "foregrounded",
        )

    // ==================== SCREEN TRACKING ====================

    data class ScreenViewed(
        val screenName: String,
        val timeSpentSeconds: Long? = null,
        val previousScreen: String? = null,
    ) : AnalyticsEvent(
            category = "screen",
            action = "viewed",
            label = screenName,
            value = timeSpentSeconds?.toDouble(),
            properties =
                mapOf(
                    "screen_name" to screenName,
                    "previous_screen" to (previousScreen ?: "none"),
                ),
        )

    data class NavigationAction(
        val fromScreen: String,
        val toScreen: String,
        val trigger: String? = null,
    ) : AnalyticsEvent(
            category = "navigation",
            action = "screen_change",
            label = "$fromScreen -> $toScreen",
            properties =
                mapOf(
                    "from_screen" to fromScreen,
                    "to_screen" to toScreen,
                    "trigger" to (trigger ?: "unknown"),
                ),
        )

    // ==================== BACKEND USAGE ====================

    data class BackendConfigured(
        val backendType: String, // "Internet Archive", "Private Server", "DWeb Service", "Storacha"
        val isNew: Boolean = true,
    ) : AnalyticsEvent(
            category = "backend",
            action = if (isNew) "configured" else "updated",
            label = backendType,
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "is_new" to isNew,
                ),
        )

    data class BackendRemoved(
        val backendType: String,
        val reason: String? = null,
    ) : AnalyticsEvent(
            category = "backend",
            action = "removed",
            label = backendType,
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "reason" to (reason ?: "unknown"),
                ),
        )

    // ==================== UPLOAD METRICS ====================

    data class UploadStarted(
        val backendType: String,
        val fileType: String, // "image", "video", "document", "other"
        val fileSizeKB: Long,
    ) : AnalyticsEvent(
            category = "upload",
            action = "started",
            label = backendType,
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "file_type" to fileType,
                    "file_size_kb" to fileSizeKB,
                    "file_size_category" to getFileSizeCategory(fileSizeKB),
                ),
        ) {
        companion object {
            internal fun getFileSizeCategory(sizeKB: Long): String =
                when {
                    sizeKB < 100 -> "tiny"

                    // < 100KB
                    sizeKB < 1024 -> "small"

                    // < 1MB
                    sizeKB < 10240 -> "medium"

                    // < 10MB
                    sizeKB < 102400 -> "large"

                    // < 100MB
                    else -> "very_large" // >= 100MB
                }
        }
    }

    data class UploadCompleted(
        val backendType: String,
        val fileType: String,
        val fileSizeKB: Long,
        val durationSeconds: Long,
        val uploadSpeedKBps: Long? = null,
    ) : AnalyticsEvent(
            category = "upload",
            action = "completed",
            label = backendType,
            value = durationSeconds.toDouble(),
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "file_type" to fileType,
                    "file_size_kb" to fileSizeKB,
                    "duration_seconds" to durationSeconds,
                    "upload_speed_kbps" to (uploadSpeedKBps ?: 0),
                    "file_size_category" to UploadStarted.getFileSizeCategory(fileSizeKB),
                ),
        )

    data class UploadFailed(
        val backendType: String,
        val fileType: String,
        val errorCategory: String, // "network", "permission", "file_not_found", "storage", "unknown"
        val fileSizeKB: Long? = null,
    ) : AnalyticsEvent(
            category = "upload",
            action = "failed",
            label = backendType,
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "file_type" to fileType,
                    "error_category" to errorCategory,
                    "file_size_kb" to (fileSizeKB ?: 0),
                ),
        )

    // ==================== MEDIA ACTIONS ====================

    data class MediaCaptured(
        val mediaType: String, // "photo", "video"
        val source: String = "camera",
    ) : AnalyticsEvent(
            category = "media",
            action = "captured",
            label = mediaType,
            properties =
                mapOf(
                    "media_type" to mediaType,
                    "source" to source,
                ),
        )

    data class MediaSelected(
        val count: Int,
        val source: String, // "gallery", "camera", "files"
        val mediaTypes: List<String> = emptyList(),
    ) : AnalyticsEvent(
            category = "media",
            action = "selected",
            label = source,
            value = count.toDouble(),
            properties =
                mapOf(
                    "count" to count,
                    "source" to source,
                    "has_images" to mediaTypes.contains("image"),
                    "has_videos" to mediaTypes.contains("video"),
                    "has_documents" to mediaTypes.contains("document"),
                ),
        )

    data class MediaDeleted(
        val count: Int,
    ) : AnalyticsEvent(
            category = "media",
            action = "deleted",
            value = count.toDouble(),
            properties = mapOf("count" to count),
        )

    // ==================== FEATURE USAGE ====================

    data class FeatureToggled(
        val featureName: String, // "proofmode", "tor", "dark_mode", "wifi_only_upload"
        val enabled: Boolean,
    ) : AnalyticsEvent(
            category = "feature",
            action = if (enabled) "enabled" else "disabled",
            label = featureName,
            properties =
                mapOf(
                    "feature_name" to featureName,
                    "enabled" to enabled,
                ),
        )

    // ==================== ERROR TRACKING ====================

    data class ErrorOccurred(
        val errorCategory: String, // "network", "permission", "upload", "auth", "storage", "unknown"
        val screenName: String,
        val backendType: String? = null,
    ) : AnalyticsEvent(
            category = "error",
            action = errorCategory,
            label = screenName,
            properties =
                mapOf(
                    "error_category" to errorCategory,
                    "screen_name" to screenName,
                    "backend_type" to (backendType ?: "none"),
                ),
        )

    // ==================== SESSION TRACKING ====================

    data class SessionStarted(
        val isFirstSession: Boolean = false,
        val sessionNumber: Int = 1,
    ) : AnalyticsEvent(
            category = "session",
            action = "started",
            value = sessionNumber.toDouble(),
            properties =
                mapOf(
                    "is_first_session" to isFirstSession,
                    "session_number" to sessionNumber,
                ),
        )

    data class SessionEnded(
        val lastScreen: String,
        val durationSeconds: Long,
        val uploadsCompleted: Int = 0,
        val uploadsFailed: Int = 0,
    ) : AnalyticsEvent(
            category = "session",
            action = "ended",
            label = lastScreen,
            value = durationSeconds.toDouble(),
            properties =
                mapOf(
                    "last_screen" to lastScreen,
                    "duration_seconds" to durationSeconds,
                    "uploads_completed" to uploadsCompleted,
                    "uploads_failed" to uploadsFailed,
                ),
        )

    // ==================== USAGE STATISTICS ====================

    data class DailyUsageStats(
        val totalUploads: Int,
        val totalUploadSizeMB: Long,
        val successRate: Float,
        val averageUploadTimeSec: Long,
        val mostUsedBackend: String,
    ) : AnalyticsEvent(
            category = "usage",
            action = "daily_stats",
            properties =
                mapOf(
                    "total_uploads" to totalUploads,
                    "total_upload_size_mb" to totalUploadSizeMB,
                    "success_rate" to successRate,
                    "average_upload_time_sec" to averageUploadTimeSec,
                    "most_used_backend" to mostUsedBackend,
                ),
        )

    // ==================== UPLOAD BATCH TRACKING ====================

    data class UploadBatchStarted(
        val count: Int,
        val totalSizeMB: Long,
    ) : AnalyticsEvent(
            category = "upload",
            action = "batch_started",
            value = count.toDouble(),
            properties =
                mapOf(
                    "count" to count,
                    "total_size_mb" to totalSizeMB,
                ),
        )

    data class UploadBatchCompleted(
        val count: Int,
        val successCount: Int,
        val failedCount: Int,
        val durationSeconds: Long,
    ) : AnalyticsEvent(
            category = "upload",
            action = "batch_completed",
            value = durationSeconds.toDouble(),
            properties =
                mapOf(
                    "count" to count,
                    "success_count" to successCount,
                    "failed_count" to failedCount,
                    "duration_seconds" to durationSeconds,
                    "success_rate" to if (count > 0) (successCount.toFloat() / count * 100).toInt() else 0,
                ),
        )

    data class UploadCancelled(
        val backendType: String,
        val fileType: String,
        val reason: String,
    ) : AnalyticsEvent(
            category = "upload",
            action = "cancelled",
            label = backendType,
            properties =
                mapOf(
                    "backend_type" to backendType,
                    "file_type" to fileType,
                    "reason" to reason,
                ),
        )

    data class UploadNetworkError(
        val reason: String, // "no_network", "wifi_required", "connection_lost"
    ) : AnalyticsEvent(
            category = "upload",
            action = "network_error",
            label = reason,
            properties = mapOf("reason" to reason),
        )

    // ==================== ENGAGEMENT TRACKING ====================

    class ReviewPromptShown :
        AnalyticsEvent(
            category = "engagement",
            action = "review_prompt_shown",
        )

    class ReviewPromptCompleted :
        AnalyticsEvent(
            category = "engagement",
            action = "review_prompt_completed",
        )

    data class ReviewPromptError(
        val errorCode: Int,
    ) : AnalyticsEvent(
            category = "engagement",
            action = "review_prompt_error",
            value = errorCode.toDouble(),
            properties = mapOf("error_code" to errorCode),
        )
}
