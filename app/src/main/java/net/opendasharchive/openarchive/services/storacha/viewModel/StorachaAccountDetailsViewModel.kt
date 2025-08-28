package net.opendasharchive.openarchive.services.storacha.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.AccountUsageResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService

class StorachaAccountDetailsViewModel(
    application: Application,
    private val apiService: StorachaApiService,
) : AndroidViewModel(application) {
    
    private val _accountUsage = MutableLiveData<Result<AccountUsageResponse>>()
    val accountUsage: LiveData<Result<AccountUsageResponse>> = _accountUsage
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _logoutResult = MutableLiveData<Result<Unit>>()
    val logoutResult: LiveData<Result<Unit>> = _logoutResult
    
    fun loadAccountUsage(sessionId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getAccountUsage(sessionId)
                _accountUsage.value = Result.success(response)
            } catch (e: Exception) {
                _accountUsage.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getUsagePercentage(totalBytes: Long, maxBytes: Long = 2L * 1024 * 1024 * 1024 * 1024): Int {
        return if (maxBytes > 0) {
            ((totalBytes.toDouble() / maxBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
    
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.2f %s", size, units[unitIndex])
    }
    
    fun formatUsageText(usedBytes: Long, maxBytes: Long = 2L * 1024 * 1024 * 1024 * 1024): String {
//        return "Used: ${formatBytes(usedBytes)} / ${formatBytes(maxBytes)}"
        return "Used: ${formatBytes(usedBytes)}"
    }
    
    fun logout(sessionId: String) {
        viewModelScope.launch {
            try {
                apiService.logout(sessionId)
                _logoutResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _logoutResult.value = Result.failure(e)
            }
        }
    }
}