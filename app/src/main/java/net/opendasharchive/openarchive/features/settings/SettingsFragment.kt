package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSettingsBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveActivity
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.getVersionName
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.styleAsLink
import kotlin.math.roundToInt

class SettingsFragment : Fragment() {

    private lateinit var mBinding: FragmentSettingsBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mBinding = FragmentSettingsBinding.inflate(inflater, container, false)


        mBinding.btGeneral.setDrawable(R.drawable.ic_account_circle, Position.Start, 0.6)
        mBinding.btGeneral.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btGeneral.setOnClickListener {
            val context = context ?: return@setOnClickListener

            startActivity(Intent(context, GeneralSettingsActivity::class.java))
        }

        mBinding.btSpace.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btSpace.setOnClickListener {
            startSpaceAuthActivity()
        }

        mBinding.btFolders.setDrawable(R.drawable.ic_folder, Position.Start, 0.6)
        mBinding.btFolders.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btFolders.setOnClickListener {
            val context = context ?: return@setOnClickListener

            startActivity(Intent(context, FoldersActivity::class.java))
        }

        mBinding.btAbout.text = getString(R.string.action_about, getString(R.string.app_name))
        mBinding.btAbout.styleAsLink()
        mBinding.btAbout.setOnClickListener {
            context?.openBrowser("https://open-archive.org/save")
        }

        mBinding.btPrivacy.styleAsLink()
        mBinding.btPrivacy.setOnClickListener {
            context?.openBrowser("https://open-archive.org/privacy")
        }

        val activity = activity

        if (activity != null) {
            mBinding.version.text = getString(
                R.string.version__,
                activity.packageManager.getVersionName(activity.packageName)
            )
        }

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        updateSpace()
    }

    private fun updateSpace() {
        val context = context ?: return
        val space = Space.current

        if (space != null) {
            mBinding.btSpace.text = space.friendlyName

            mBinding.btSpace.setDrawable(
                space.getAvatar(context)?.scaled(24, context),
                Position.Start, tint = true
            )
        } else {
            mBinding.btSpace.visibility = View.GONE
        }
    }

    private fun startSpaceAuthActivity() {
        val space = Space.current ?: return

        val clazz = when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> InternetArchiveActivity::class.java
            Space.Type.GDRIVE -> GDriveActivity::class.java
            else -> WebDavActivity::class.java
        }

        val intent = Intent(context, clazz)
        intent.putExtra(BaseActivity.EXTRA_DATA_SPACE, space.id)

        startActivity(intent)
    }
}