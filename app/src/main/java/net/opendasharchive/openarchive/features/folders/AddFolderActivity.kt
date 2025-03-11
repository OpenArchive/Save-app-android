package net.opendasharchive.openarchive.features.folders

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity

class AddFolderActivity : BaseActivity() {

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
        const val EXTRA_FOLDER_NAME = "folder_name"
    }

    private lateinit var mResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    setResult(RESULT_OK, it.data)
                    finish()
                } else {
                    val name = it.data?.getStringExtra(EXTRA_FOLDER_NAME)

                    if (!name.isNullOrBlank()) {
                        val i = Intent(this, CreateNewFolderFragment::class.java)
                        i.putExtra(EXTRA_FOLDER_NAME, name)

                        mResultLauncher.launch(i)
                    }
                }
            }

        //mBinding = ActivityAddFolderBinding.inflate(layoutInflater)
        //setContentView(mBinding.root)


        setContent {

            SaveAppTheme {

                AddFolderScreen(
//                    onCreateFolder = {
//                        setFolder(browse = false)
//                    },
//                    onBrowseFolders = {
//                        setFolder(browse = true)
//                    },
//                    onNavigateBack = {
//                        finish()
//                    }
                )
            }
        }




        // We cannot browse the Internet Archive. Directly forward to creating a project,
        // as it doesn't make sense to show a one-option menu.
        if (Space.current?.tType == Space.Type.INTERNET_ARCHIVE) {
            //mBinding.browseFolderContainer.hide()

            finish()
            setFolder(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setFolder(browse: Boolean) {
        if (Space.current == null) {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))

            return
        }

        mResultLauncher.launch(
            Intent(
                this,
                if (browse) BrowseFoldersFragment::class.java else CreateNewFolderFragment::class.java
            )
        )
    }
}