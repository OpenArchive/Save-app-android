package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdCreateGroupBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.util.FullScreenOverlayCreateGroupManager
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import java.io.File

class SnowbirdCreateGroupFragment : BaseFragment() {

    private lateinit var binding: FragmentSnowbirdCreateGroupBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSnowbirdCreateGroupBinding.inflate(inflater)

        binding.buttonBar.applyEdgeToEdgeInsets(WindowInsetsCompat.Type.navigationBars()) { insets ->
            bottomMargin = insets.bottom
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            snowbirdGroupViewModel.createGroup(binding.groupNameTextfield.text.toString())
            dismissKeyboard(it)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        initializeViewModelObservers()
        setupTextWatchers()
        if (BuildConfig.DEBUG) {
            //setupFilesList()
        }
    }

    private fun initializeViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    snowbirdGroupViewModel.groupState.collectLatest { state ->
                        handleGroupStateUpdate(
                            state
                        )
                    }
                }
                launch {
                    snowbirdRepoViewModel.repoState.collectLatest { state ->
                        handleRepoStateUpdate(
                            state
                        )
                    }
                }
            }
        }
    }

    private fun handleGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        AppLogger.d("group state = $state")
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> handleCreateGroupLoadingStatus(true)
            is SnowbirdGroupViewModel.GroupState.SingleGroupSuccess -> handleGroupCreated(state.group)
            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun handleCreateGroupLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayCreateGroupManager.show(this@SnowbirdCreateGroupFragment)
        } else {
            FullScreenOverlayCreateGroupManager.hide()
        }
    }


    private fun handleRepoStateUpdate(state: SnowbirdRepoViewModel.RepoState) {
        AppLogger.d("repo state = $state")
        when (state) {
            is SnowbirdRepoViewModel.RepoState.Loading -> handleCreateGroupLoadingStatus(true)
            is SnowbirdRepoViewModel.RepoState.SingleRepoSuccess -> handleRepoCreated(state.repo)
            is SnowbirdRepoViewModel.RepoState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    override fun handleError(error: SnowbirdError) {
        handleCreateGroupLoadingStatus(false)
        super.handleError(error)
    }

    private fun handleGroupCreated(group: SnowbirdGroup?) {
        if (group == null) {
            handleError(SnowbirdError.GeneralError("Group was null"))
            return
        }

        snowbirdGroupViewModel.setCurrentGroup(group)

        lifecycleScope.launch {
            group.save()
            snowbirdRepoViewModel.createRepo(
                groupKey = group.key,
                repoName = binding.repoNameTextfield.text.toString()
            )
        }
    }

    private fun handleRepoCreated(repo: SnowbirdRepo?) {
        handleCreateGroupLoadingStatus(false)
        if (repo == null) {
            handleError(SnowbirdError.GeneralError("Repo was null"))
            return
        }

        repo.groupKey = snowbirdGroupViewModel.currentGroup.value!!.key
        repo.permissions = "READ_WRITE"
        repo.save()
        showConfirmation(repo)
    }

    private fun showConfirmation(repo: SnowbirdRepo?) {
        val group = SnowbirdGroup.get(repo!!.groupKey)

        if (group == null) {
            handleError(SnowbirdError.GeneralError("Group was null"))
            return
        }

        val action = SnowbirdCreateGroupFragmentDirections
            .actionFragmentSnowbirdCreateGroupToFragmentSpaceSetupSuccess(
                message = getString(R.string.you_have_successfully_created_dweb),
                spaceType = Space.Type.RAVEN,
                dwebGroupKey = group.key,
            )
        findNavController().navigate(action)
    }

    private fun setupTextWatchers() {
        // Create a common TextWatcher for all three fields
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAuthenticateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {
                dismissCredentialsError()
            }
        }

        binding.groupNameTextfield.addTextChangedListener(textWatcher)
        binding.repoNameTextfield.addTextChangedListener(textWatcher)
    }

    private fun updateAuthenticateButtonState() {
        val groupName = binding.groupNameTextfield.text?.toString()?.trim().orEmpty()
        val repoName = binding.repoNameTextfield.text?.toString()?.trim().orEmpty()

        // Enable the button only if none of the fields are empty
        binding.btnNext.isEnabled = groupName.isNotEmpty() && repoName.isNotEmpty()
    }

    private fun dismissCredentialsError() {
        //binding.errorHint.hide()
    }

    private fun setupFilesList() {
        try {
            val filesDir = requireContext().filesDir
            val files = filesDir.listFiles()
            
            val fileInfoList = mutableListOf<String>()
            
            if (files != null) {
                // Add socket file specifically if it exists
                val socketFile = File(filesDir, "rust_server.sock")
                if (socketFile.exists()) {
                    fileInfoList.add("🔗 rust_server.sock (${formatFileSize(socketFile.length())})")
                }
                
                // Add other files
                files.filter { it.name != "rust_server.sock" }.forEach { file ->
                    val icon = if (file.isDirectory()) "📁" else "📄"
                    val size = if (file.isDirectory()) "" else " (${formatFileSize(file.length())})"
                    fileInfoList.add("$icon ${file.name}$size")
                }
                
                if (fileInfoList.isEmpty()) {
                    fileInfoList.add("(No files found)")
                }
            } else {
                fileInfoList.add("(Unable to access files directory)")
            }
            
            // Update the label to show directory path
            binding.filesLabel.text = "App Files Directory (${filesDir.absolutePath}):"
            binding.filesList.visibility = View.VISIBLE
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                fileInfoList
            )
            binding.filesList.adapter = adapter
            binding.filesList.visibility = View.VISIBLE
            
        } catch (e: Exception) {
            AppLogger.e("Error listing app files", e)
            val errorList = listOf("Error loading files: ${e.message}")
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                errorList
            )
            binding.filesList.adapter = adapter
            binding.filesList.visibility = View.VISIBLE
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes bytes"
        }
    }

    override fun getToolbarTitle(): String {
        return "Create DWeb Storage Group"
    }

}