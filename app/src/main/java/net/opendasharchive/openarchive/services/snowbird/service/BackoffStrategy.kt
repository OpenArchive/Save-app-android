package net.opendasharchive.openarchive.services.snowbird.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Defines different backoff strategies for retry attempts.
 */
sealed class BackoffStrategy {
    /**
     * @param baseDelay Base delay duration between retries
     */
    data class Linear(val baseDelay: Duration = 1.seconds) : BackoffStrategy()

    /**
     * @param baseDelay Initial delay duration
     * @param multiplier Factor to multiply delay by on each attempt
     * @param maxDelay Maximum delay between retries
     */
    data class Exponential(
        val baseDelay: Duration = 1.seconds,
        val multiplier: Double = 2.0,
        val maxDelay: Duration = 60.seconds
    ) : BackoffStrategy()
}