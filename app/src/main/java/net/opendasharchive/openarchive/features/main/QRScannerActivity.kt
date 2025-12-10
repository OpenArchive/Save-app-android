package net.opendasharchive.openarchive.features.main

import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity
import timber.log.Timber

class QRScannerActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("Starting QR scanner")
    }
}