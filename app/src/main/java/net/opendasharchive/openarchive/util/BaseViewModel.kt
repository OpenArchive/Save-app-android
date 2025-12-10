package net.opendasharchive.openarchive.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.RefreshGroupResponse
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdResult

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val processingTracker = ProcessingTracker()

    private val _error = MutableStateFlow<SnowbirdError?>(null)
    val error: StateFlow<SnowbirdError?> = _error.asStateFlow()
}