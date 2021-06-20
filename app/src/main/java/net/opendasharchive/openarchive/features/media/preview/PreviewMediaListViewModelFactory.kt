package net.opendasharchive.openarchive.features.media.preview

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class PreviewMediaListViewModelFactory(
    private val repository: MediaRepository,
    private val context: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PreviewMediaListViewModel::class.java)) {
            return PreviewMediaListViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}