package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner.QRScannerScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment

class SnowbirdQRScannerFragment : BaseSnowbirdFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    QRScannerScreen(
                        onQrCodeScanned = { result ->
                            findNavController().previousBackStackEntry?.savedStateHandle?.set(QR_RESULT_KEY, result)
                            findNavController().popBackStack()
                        },
                        onNavigateBack = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.scan_qr_code)
    }

    override fun isToolbarVisible(): Boolean {
        return false
    }

    companion object {
        const val QR_RESULT_KEY = "qr_scan_result"
    }
}
