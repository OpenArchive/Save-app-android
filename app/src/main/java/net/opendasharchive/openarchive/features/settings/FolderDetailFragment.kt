package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentFolderDetailBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class FolderDetailFragment : BaseFragment() {

    private val useComposeImplementation = true

    private val args: FolderDetailFragmentArgs by navArgs()

    private lateinit var mProject: Project
    private lateinit var mBinding: FragmentFolderDetailBinding

    private val viewModel: FolderDetailViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (useComposeImplementation) {
            return ComposeView(requireContext()).apply {
                setContent {
                    SaveAppTheme {
                        FolderDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                findNavController().popBackStack()
                            }
                        )
                    }
                }
            }
        }

        mBinding = FragmentFolderDetailBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (useComposeImplementation) {
            // Pass projectId to ViewModel
            viewModel.setProjectId(args.currentProjectId)
            return
        }

        // Get arguments from Navigation Component
        mProject = Project.getById(args.currentProjectId)!!

        setupEditorListeners()
        setupButtonListeners()
        setupLicenseManager()
        updateUi()
    }

    private fun setupEditorListeners() {
        mBinding.folderName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = mBinding.folderName.text.toString()

                if (newName.isNotBlank()) {
                    mProject.description = newName
                    mProject.save()

                    mBinding.folderName.hint = newName
                }
            }

            false
        }
    }

    private fun setupButtonListeners() {

        mBinding.btRemove.setOnClickListener {
            showDeleteFolderConfirmDialog()
        }

        mBinding.btArchive.setOnClickListener {
            unArchiveProject()
        }
    }

    private fun setupLicenseManager() {
        CreativeCommonsLicenseManager.initialize(mBinding.cc, null) {
            mProject.licenseUrl = it
            mProject.save()
        }
    }

    private fun showDeleteFolderConfirmDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            icon = UiImage.DrawableResource(R.drawable.ic_trash)
            title = UiText.StringResource(R.string.remove_from_app)
            message = UiText.StringResource(R.string.action_remove_project)
            destructiveButton {
                text = UiText.StringResource(R.string.lbl_remove)
                action = {
                    mProject.delete()

                    findNavController().popBackStack()
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.lbl_Cancel)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }
    }

    private fun unArchiveProject() {
        mProject.isArchived = false
        mProject.save()

        findNavController().popBackStack()
    }

    private fun updateUi() {

        mBinding.folderName.isEnabled = !mProject.isArchived
        mBinding.folderName.hint = mProject.description
        mBinding.folderName.setText(mProject.description)

        mBinding.btArchive.setText(if (mProject.isArchived)
            R.string.action_unarchive_project else
            R.string.action_archive_project)

        val global = mProject.space?.license != null

        if (global) {
            mBinding.cc.tvCcLabel.setText(R.string.set_the_same_creative_commons_license_for_all_folders_on_this_server)
        }

        CreativeCommonsLicenseManager.initialize(mBinding.cc, mProject.licenseUrl, !mProject.isArchived && !global)
    }

    override fun getToolbarTitle(): String = "Edit Folder"
    override fun shouldShowBackButton() = true
}