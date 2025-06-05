package net.opendasharchive.openarchive.services.snowbird.service

import kotlin.coroutines.CoroutineContext

/**
 * Configuration for retry behavior
 * @param maxAttempts Maximum number of retry attempts (null for infinite)
 * @param backoffStrategy Strategy to use for delay between retries
 */
data class RetryConfig(
    val maxAttempts: Int? = null,
    val backoffStrategy: BackoffStrategy = BackoffStrategy.Exponential(),
    val context: CoroutineContext? = null
)