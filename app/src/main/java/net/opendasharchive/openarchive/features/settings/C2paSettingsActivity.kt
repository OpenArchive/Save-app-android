package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import net.opendasharchive.openarchive.features.core.BaseActivity

class C2paSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            C2paScreen(
                onNavigateBack = {
                    finish()
                }
            )
        }
    }
}
