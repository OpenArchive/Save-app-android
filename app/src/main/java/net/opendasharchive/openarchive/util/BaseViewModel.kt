package net.opendasharchive.openarchive.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.core.domain.DomainError

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val processingTracker = ProcessingTracker()

    private val _error = MutableStateFlow<DomainError?>(null)
    val error: StateFlow<DomainError?> = _error.asStateFlow()
}