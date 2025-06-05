package net.opendasharchive.openarchive.services.gdrive

import android.os.Bundle
import android.view.MenuItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityGdriveBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper

class GDriveActivity : BaseActivity() {

    private lateinit var mBinding: ActivityGdriveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var space: Space? = null

        if (intent.hasExtra(EXTRA_DATA_SPACE)) {
            space = Space.get(intent.getLongExtra(EXTRA_DATA_SPACE, -1L))
        }

        mBinding = ActivityGdriveBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btRemove.setOnClickListener {
            if (space != null) removeSpace(space)
        }

        setupToolbar(getString(R.string.gdrive))

        mBinding.gdriveId.setText(space?.displayname ?: "")
    }

    private fun removeSpace(space: Space) {
        AlertHelper.show(this, R.string.are_you_sure_you_want_to_remove_this_server_from_the_app, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.remove) { _, _ ->
                // delete sign-in from database
                space.delete()

                // google logout
                val googleSignInClient =
                    GoogleSignIn.getClient(applicationContext, GoogleSignInOptions.DEFAULT_SIGN_IN)
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    googleSignInClient.signOut()
                }

                // leave activity
                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }

    // boilerplate to make back button in app bar work
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}