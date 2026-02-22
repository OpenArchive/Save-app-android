package net.opendasharchive.openarchive.services.storacha

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.features.core.ToolbarAction

data class StorachaToolbarConfig(
    val title: String,
    val showBack: Boolean,
    val actions: List<ToolbarAction> = emptyList(),
)

object StorachaToolbarState {
    private val _config = MutableStateFlow(StorachaToolbarConfig(title = "", showBack = false))
    val config: StateFlow<StorachaToolbarConfig> = _config.asStateFlow()

    fun update(config: StorachaToolbarConfig) {
        _config.value = config
    }
}
