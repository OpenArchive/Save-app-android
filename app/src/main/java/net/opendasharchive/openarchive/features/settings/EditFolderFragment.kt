package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentEditFolderBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.NavArgument
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog

class EditFolderFragment : BaseFragment() {

    private lateinit var project: Project
    private lateinit var binding: FragmentEditFolderBinding

    override fun getToolbarTitle(): String = "Edit Folder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val folderId = it.getLong(NavArgument.FOLDER_ID)
            if (folderId == -1L) {
                throw IllegalArgumentException("Folder ID cannot be -1")
            }
            project = Project.getById(folderId) ?:
                throw IllegalArgumentException("Project not found for ID: $folderId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.folderName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = binding.folderName.text.toString()

                if (newName.isNotBlank()) {
                    project.description = newName
                    project.save()

                    //TODO: update toolbar title
                    //supportActionBar?.title = newName
                    binding.folderName.hint = newName


                    //setupToolbar(newName)
                }
            }

            false
        }

        binding.btRemove.setOnClickListener {
            showDeleteFolderConfirmDialog()
        }

        binding.btArchive.setOnClickListener {
            archiveProject()
        }

        CreativeCommonsLicenseManager.initialize(binding.cc, null) {
            project.licenseUrl = it
            project.save()
        }

        updateUi()
    }

    private fun showDeleteFolderConfirmDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            icon = UiImage.DrawableResource(R.drawable.ic_trash)
            title = UiText.StringResource(R.string.remove_from_app)
            message = UiText.StringResource(R.string.action_remove_project)
            destructiveButton {
                text = UiText.StringResource(R.string.remove)
                action = {
                    project.delete()
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

    private fun archiveProject() {
        project.isArchived = !project.isArchived
        project.save()

        updateUi()
    }

    private fun updateUi() {
        //supportActionBar?.title = project.description

        binding.folderName.isEnabled = !project.isArchived
        binding.folderName.hint = project.description
        binding.folderName.setText(project.description)

        binding.btArchive.setText(if (project.isArchived)
            R.string.action_unarchive_project else
            R.string.action_archive_project)

        val global = project.space?.license != null

        if (global) {
            binding.cc.tvCcLabel.setText(R.string.set_the_same_creative_commons_license_for_all_folders_on_this_server)
        }

        CreativeCommonsLicenseManager.initialize(binding.cc, project.licenseUrl, !project.isArchived && !global)
    }
}