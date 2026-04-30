package net.opendasharchive.openarchive.features.main.ui

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedImportState {
    private val _pendingUris = MutableStateFlow<List<Uri>?>(null)
    val pendingUris: StateFlow<List<Uri>?> = _pendingUris.asStateFlow()

    fun setPendingUris(uris: List<Uri>) {
        _pendingUris.value = uris
    }

    fun clear() {
        _pendingUris.value = null
    }
}
