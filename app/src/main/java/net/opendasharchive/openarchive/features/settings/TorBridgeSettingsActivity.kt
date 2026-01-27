package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import net.opendasharchive.openarchive.features.core.BaseActivity

class TorBridgeSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TorBridgeScreen(
                onNavigateBack = {
                    finish()
                }
            )
        }
    }
}
