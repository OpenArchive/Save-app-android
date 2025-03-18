package net.opendasharchive.openarchive.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import net.opendasharchive.openarchive.extensions.formatToDecimalPlaces
import timber.log.Timber
import kotlin.time.measureTimedValue

class ProcessingTracker(private val logger: (String) -> Unit = { Timber.d(it) }) {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun <T> track(taskName: String = "Unnamed task", block: suspend () -> T): T {
        _isProcessing.value = true
        try {
            val (result, duration) = measureTimedValue {
                block()
            }
            logger("$taskName completed in ${duration.formatToDecimalPlaces(3)} seconds")
            return result
        } finally {
            _isProcessing.value = false
        }
    }
}

// Extension function for basic tracking with logging
suspend fun <T> ProcessingTracker.trackProcessing(
    taskName: String = "Unnamed task",
    block: suspend () -> T
): T = track(taskName, block)

// Extension function with timeout and logging
suspend fun <T> ProcessingTracker.trackProcessingWithTimeout(
    timeoutMs: Long,
    taskName: String = "Unnamed task",
    block: suspend () -> T
): T = withTimeout(timeoutMs) {
    track(taskName, block)
}