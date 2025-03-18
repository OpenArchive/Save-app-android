package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityConsentBinding
import net.opendasharchive.openarchive.features.core.BaseActivity

class ConsentActivity: BaseActivity() {

    private lateinit var mBinding: ActivityConsentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityConsentBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(getString(R.string.health_checks))

        mBinding.explainer.text = getString(
            R.string.by_allowing_health_checks_you_give_permission_for_the_app_to_securely_send_health_check_data_to_the_s_team,
            getString(R.string.app_name))

        mBinding.cancelButton.setOnClickListener {
            finishDeny()
        }

        mBinding.okButton.setOnClickListener {
            finish()

            CleanInsightsManager.grant()
        }

        onBackPressedDispatcher.addCallback {
            finishDeny()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finishDeny()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun finishDeny() {
        finish()

        CleanInsightsManager.deny()
    }
}