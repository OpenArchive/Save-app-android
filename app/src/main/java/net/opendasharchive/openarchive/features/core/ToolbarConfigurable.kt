package net.opendasharchive.openarchive.features.core

data class ToolbarAction(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
)

interface ToolbarConfigurable {
    fun getToolbarTitle(): String
    fun getToolbarSubtitle(): String? = null
    fun shouldShowBackButton(): Boolean = true
    fun isToolbarVisible(): Boolean = true
    fun getToolbarActions(): List<ToolbarAction> = emptyList()
}