package net.opendasharchive.openarchive.features.folders

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentBrowseFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.dialog.showSuccessDialog
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date

class BrowseFoldersFragment : BaseFragment(), MenuProvider {

    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = false  // Set to false to use XML implementation

    private lateinit var binding: FragmentBrowseFoldersBinding
    private val mViewModel: BrowseFoldersViewModel by viewModel()

    private var mSelected: Folder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (useComposeImplementation) {
            // Use Compose implementation
            return ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SaveAppTheme {
                        BrowseFolderScreen(
                            viewModel = mViewModel,
                            onNavigateBackWithResult = { projectId ->
                                requireActivity().setResult(RESULT_OK, Intent().apply {
                                    putExtra(SpaceSetupActivity.EXTRA_FOLDER_ID, projectId)
                                })
                                requireActivity().finish()
                            },
                            onFolderSelected = { folder ->
                                mSelected = folder
                                activity?.invalidateOptionsMenu()
                            }
                        )
                    }
                }
            }
        }

        // Original XML implementation
        binding = FragmentBrowseFoldersBinding.inflate(layoutInflater)

        binding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFolderList.clipToPadding = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Skip XML-specific setup when using Compose
        if (useComposeImplementation) return

        ViewCompat.setOnApplyWindowInsetsListener(binding.rvFolderList) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.updatePadding(
                bottom = insets.bottom + view.paddingBottom
            )

            windowInsets
        }

        val space = Space.current
        //if (space != null) mViewModel.getFiles(space)

        mViewModel.folders.observe(viewLifecycleOwner) {
            binding.projectsEmpty.toggle(it.isEmpty())

            binding.rvFolderList.adapter = BrowseFoldersAdapter(it) { folder ->
                this.mSelected = folder
                activity?.invalidateOptionsMenu()
            }
        }

        mViewModel.progressBarFlag.observe(viewLifecycleOwner) {
            binding.progressBar.toggle(it)
        }
    }


    override fun getToolbarTitle(): String = getString(R.string.browse_existing)

    private fun addFolder(folder: Folder?) {
        if (folder == null) return
        val space = Space.current ?: return

        // This should not happen. These should have been filtered on display.
        if (space.hasProject(folder.name)) return

        val license = space.license


        val project = Project(folder.name, Date(), space.id, licenseUrl = license)
        project.save()

        showFolderCreated(project.id)
    }

    private fun showFolderCreated(projectId: Long) {
        dialogManager.showSuccessDialog(
            title = R.string.label_success_title,
            message = R.string.create_folder_ok_message,
            positiveButtonText = R.string.label_got_it,
            onDone = {
                navigateBackWithResult(projectId)
            },
            onDismissed = {
                // If the dialog is dismissed, we still want to navigate back
                navigateBackWithResult(projectId)
            }
        )
    }

    private fun navigateBackWithResult(projectId: Long) {
        requireActivity().setResult(RESULT_OK, Intent().apply {
            putExtra(SpaceSetupActivity.EXTRA_FOLDER_ID, projectId)
        })
        requireActivity().finish()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val addMenuItem = menu.findItem(R.id.action_add)
        addMenuItem?.isVisible = mSelected != null
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_add -> {
                if (useComposeImplementation) {
                    // Trigger the action through ViewModel
                    mViewModel.onAction(BrowseFoldersAction.AddFolder)
                } else {
                    // Use legacy method
                    addFolder(mSelected)
                }
                true
            }

            else -> false
        }
    }
}
