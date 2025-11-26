package net.opendasharchive.openarchive.features.core

interface ToolbarConfigurable {
    fun getToolbarTitle(): String
    fun getToolbarSubtitle(): String? = null
    fun shouldShowBackButton(): Boolean = true
}