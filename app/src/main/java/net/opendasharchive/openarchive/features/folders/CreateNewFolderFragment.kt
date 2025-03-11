package net.opendasharchive.openarchive.features.folders

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.MenuProvider
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentCreateNewFolderBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.dialog.showSuccessDialog
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import net.opendasharchive.openarchive.util.extensions.hide
import java.util.Date

class CreateNewFolderFragment : BaseFragment() {

    companion object {
        private const val SPECIAL_CHARS = ".*[\\\\/*\\s]"
    }

    private lateinit var binding: FragmentCreateNewFolderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateNewFolderBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val intent = requireActivity().intent

        binding.newFolder.setText(intent.getStringExtra(AddFolderActivity.EXTRA_FOLDER_NAME))

        binding.newFolder.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                store()
            }

            false
        }

        binding.btnSubmit.setOnClickListener {
            store()
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().setResult(RESULT_CANCELED)
            requireActivity().finish()
        }

        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        // Create a common TextWatcher for all three fields
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAuthenticateButtonState()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.newFolder.addTextChangedListener(textWatcher)
    }

    private fun updateAuthenticateButtonState() {
        val folderName = binding.newFolder.text?.toString()?.trim().orEmpty()

        // Enable the button only if none of the fields are empty
        binding.btnSubmit.isEnabled = folderName.isNotEmpty()
    }

    override fun getToolbarTitle(): String = getString(R.string.create_a_new_folder)

    private fun store() {
        val name = binding.newFolder.text.toString()

        if (name.isBlank()) return

        if (name.matches(SPECIAL_CHARS.toRegex())) {
            Toast.makeText(
                requireContext(),
                getString(R.string.please_do_not_include_special_characters_in_the_name),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val space = Space.current ?: return

        if (space.hasProject(name)) {
            Toast.makeText(
                requireContext(), getString(R.string.folder_name_already_exists),
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val license =
            space.license ?: CreativeCommonsLicenseManager.getSelectedLicenseUrl(binding.cc)

        val project = Project(name, Date(), space.id, licenseUrl = license)
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
            }
        )
    }

    private fun navigateBackWithResult(projectId: Long) {
        val i = Intent()
        i.putExtra(AddFolderActivity.EXTRA_FOLDER_ID, projectId)

        requireActivity().setResult(RESULT_OK, i)
        requireActivity().finish()
    }
}
