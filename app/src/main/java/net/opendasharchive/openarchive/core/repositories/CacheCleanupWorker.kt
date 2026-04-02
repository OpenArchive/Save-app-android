package net.opendasharchive.openarchive.core.repositories

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that keeps the app's cache directory from growing unbounded.
 *
 * Policy:
 *  - Delete any file not accessed in the last [MAX_AGE_DAYS] days
 *  - If total size still exceeds [MAX_CACHE_SIZE_BYTES], evict oldest files first
 *  - Remove any empty directories left behind
 */
class CacheCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "cache_cleanup"
        val REPEAT_INTERVAL_DAYS = 7L
        private val MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
        private const val MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024L  // 100 MB
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cacheDir = applicationContext.cacheDir
            deleteOldFiles(cacheDir)
            enforceSizeCap(cacheDir)
            AppLogger.i("CacheCleanupWorker: completed")
            Result.success()
        } catch (e: Exception) {
            AppLogger.e("CacheCleanupWorker: failed", e)
            Result.retry()
        }
    }

    private fun deleteOldFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir.walkTopDown()
            .filter { it.isFile && it.lastModified() < cutoff }
            .forEach { file ->
                if (file.delete()) {
                    AppLogger.d("CacheCleanup: deleted stale file ${file.name}")
                }
            }
        // Remove empty directories (bottom-up so children are processed before parents)
        dir.walkBottomUp()
            .filter { it.isDirectory && it != dir && it.listFiles()?.isEmpty() == true }
            .forEach { it.delete() }
    }

    private fun enforceSizeCap(dir: File) {
        val files = dir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.lastModified() }  // oldest first for eviction
            .toMutableList()

        var totalSize = files.sumOf { it.length() }
        for (file in files) {
            if (totalSize <= MAX_CACHE_SIZE_BYTES) break
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
                AppLogger.d("CacheCleanup: evicted ${file.name} to enforce size cap")
            }
        }
    }
}
