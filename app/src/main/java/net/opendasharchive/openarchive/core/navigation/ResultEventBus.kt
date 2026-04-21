package net.opendasharchive.openarchive.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Local for receiving results in a [ResultEventBus]
 */
object LocalResultEventBus {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus> =
        compositionLocalOf { ResultEventBus }

    /**
     * The current [ResultEventBus]
     */
    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current

    /**
     * Provides a [ResultEventBus] to the composition
     */
    infix fun provides(
        bus: ResultEventBus
    ): ProvidedValue<ResultEventBus> {
        return LocalResultEventBus.provides(bus)
    }
}

/**
 * An EventBus for passing results between multiple sets of screens.
 *
 * It provides a solution for event based results.
 */
object ResultEventBus {
    val channelMap: MutableMap<String, Channel<Any?>> = mutableMapOf()

    @PublishedApi internal fun getOrCreate(resultKey: String): Channel<Any?> =
        channelMap.getOrPut(resultKey) {
            Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }

    /**
     * Always returns a non-null flow. The channel is created eagerly so that
     * subscribers established before [sendResult] is called still receive events.
     */
    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()) =
        getOrCreate(resultKey).receiveAsFlow()

    inline fun <reified T> sendResult(resultKey: String = T::class.toString(), result: T) {
        getOrCreate(resultKey).trySend(result)
    }

    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        channelMap.remove(resultKey)
    }
}
