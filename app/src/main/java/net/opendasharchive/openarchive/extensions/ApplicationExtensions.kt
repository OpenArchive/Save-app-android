package net.opendasharchive.openarchive.extensions

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import org.koin.android.ext.android.getKoin
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.viewModel

inline fun <reified T : ViewModel> Application.getViewModel(vararg parameters: Any): T {
    return getKoin().get { parametersOf(*parameters) }
}

inline fun <reified T : AndroidViewModel> Fragment.androidViewModel(): Lazy<T> {
    return viewModel { parametersOf(requireActivity().application) }
}

inline fun <reified T : AndroidViewModel> ComponentActivity.androidViewModel(): Lazy<T> {
    return viewModel { parametersOf(application) }
}