package net.opendasharchive.openarchive.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.db.SnowbirdError

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val processingTracker = ProcessingTracker()

    private val _error = MutableStateFlow<SnowbirdError?>(null)
    val error: StateFlow<SnowbirdError?> = _error.asStateFlow()
}