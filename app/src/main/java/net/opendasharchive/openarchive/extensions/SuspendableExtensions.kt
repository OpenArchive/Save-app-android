package net.opendasharchive.openarchive.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.snowbird.service.BackoffStrategy
import net.opendasharchive.openarchive.services.snowbird.service.RetryConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow

// Syntactic sugar
//
fun <T> suspendToRetry(block: suspend () -> T): suspend () -> T = block

/**
 * Extension function to add retry capability to any suspend function
 */
fun <T> (suspend () -> T).withRetry(
    config: RetryConfig,
    shouldRetry: (Throwable) -> Boolean = { true }
): Flow<RetryAttempt<T>> = flow {
    var attempt = 1

    while (true) {
        try {
            val result: T = this@withRetry()
            emit(RetryAttempt.Success(result, attempt))
            break
        } catch (e: Throwable) {
            if (!shouldRetry(e) || (config.maxAttempts != null && attempt >= config.maxAttempts)) {
                emit(RetryAttempt.Failure(e, attempt))
                break
            }

            emit(RetryAttempt.Retry(e, attempt))

            val delay = when (val strategy = config.backoffStrategy) {
                is BackoffStrategy.Linear -> {
                    strategy.baseDelay * attempt
                }
                is BackoffStrategy.Exponential -> {
                    (strategy.baseDelay * strategy.multiplier.pow(attempt - 1))
                        .coerceAtMost(strategy.maxDelay)
                }
            }

            delay(delay)
            attempt++
        }
    }
}

/**
 * Extension function to add retry capability with lifecycle awareness
 * @param scope The CoroutineScope to run within (e.g., viewModelScope)
 * @param config Retry configuration
 * @param shouldRetry Predicate to determine if an exception should trigger a retry
 * @param onEach Optional callback for each attempt
 * @return Job that can be cancelled
 */
fun <T> (suspend () -> T).retryWithScope(
    scope: CoroutineScope,
    config: RetryConfig,
    shouldRetry: (Throwable) -> Boolean = { true },
    onEach: (RetryAttempt<T>) -> Unit
): Job = scope.launch(config.context ?: scope.coroutineContext) {
    withRetry(config, shouldRetry)
        .catch { throwable ->
            // Handle cancellation explicitly
            if (throwable is CancellationException) {
                throw throwable
            }
            emit(RetryAttempt.Failure(throwable, 0))
        }
        .collect { attempt ->
            onEach(attempt)
        }
}

/**
 * Interface to ensure type safety for successful results
 */
interface RetryResult<out T> {
    val result: T
    val attempt: Int
}

/**
 * Represents the state of a retry attempt
 */
sealed interface RetryAttempt<out T> {
    val attempt: Int

    data class Success<T>(override val result: T, override val attempt: Int) : RetryAttempt<T>, RetryResult<T>
    data class Failure(val error: Throwable, override val attempt: Int) : RetryAttempt<Nothing>
    data class Retry(val error: Throwable, override val attempt: Int) : RetryAttempt<Nothing>
}