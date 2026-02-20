package net.opendasharchive.openarchive.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.filterNotNull

/**
 * Composable effect for receiving results from other screens via [ResultEventBus].
 *
 * Usage:
 * ```
 * ResultEffect<List<Uri>>(resultBus) { uris ->
 *     // Handle received URIs
 *     viewModel.importMedia(uris)
 * }
 * ```
 */
@Composable
inline fun <reified T> ResultEffect(
    resultBus: ResultEventBus = ResultEventBus,
    resultKey: String = T::class.toString(),
    crossinline onResult: (T) -> Unit
) {
    LaunchedEffect(resultKey) {
        resultBus.getResultFlow<T>(resultKey)
            ?.filterNotNull()
            ?.collect { result ->
                onResult(result as T)
            }
    }
}
