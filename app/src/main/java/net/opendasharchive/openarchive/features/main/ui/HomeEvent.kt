package net.opendasharchive.openarchive.features.main.ui

import net.opendasharchive.openarchive.features.media.AddMediaType

sealed class HomeEvent {
    data class NavigateToProject(val projectId: Long) : HomeEvent()
    data class LaunchPicker(val type: AddMediaType) : HomeEvent() // Launch native picker
    data class ShowMessage(val message: String) : HomeEvent()
}