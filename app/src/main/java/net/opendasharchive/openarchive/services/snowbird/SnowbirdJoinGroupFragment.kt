package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdJoinGroupBinding
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdRepo
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.extensions.showKeyboard
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.util.FullScreenOverlayCreateGroupManager
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import timber.log.Timber

class SnowbirdJoinGroupFragment: BaseSnowbirdFragment() {

    private lateinit var binding: FragmentSnowbirdJoinGroupBinding
    private lateinit var uriString: String
    private lateinit var groupName: String
    private lateinit var repoName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            uriString = it.getString(DWEB_GROUP_KEY, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSnowbirdJoinGroupBinding.inflate(inflater)


        binding.buttonBar.applyEdgeToEdgeInsets(WindowInsetsCompat.Type.navigationBars()) { insets ->
            bottomMargin = insets.bottom
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupName = uriString.getQueryParameter("name") ?: "Unknown group"

        Timber.d("uriString = $uriString")
        Timber.d("groupName = $groupName")

        binding.groupNameTextfield.setText(groupName)

        setupViewModelObservers()
        setupSideEffects()
        setupTextWatchers()
    }

    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { snowbirdGroupViewModel.groupState.collect { state -> onGroupStateUpdate(state) } }
                launch { snowbirdRepoViewModel.repoState.collect { state -> onRepoStateUpdate(state) } }
            }
        }
    }

    override fun handleError(error: SnowbirdError) {
        handleCreateGroupLoadingStatus(false)
        super.handleError(error)
    }

    private fun onGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        Timber.d("state = $state")
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> onLoading()
            is SnowbirdGroupViewModel.GroupState.JoinGroupSuccess -> onJoinSuccess(state.group.group)
            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun onRepoStateUpdate(state: SnowbirdRepoViewModel.RepoState) {
        Timber.d("state = $state")
        when (state) {
            is SnowbirdRepoViewModel.RepoState.Loading -> onLoading()
            is SnowbirdRepoViewModel.RepoState.SingleRepoSuccess -> onRepoCreated(state.groupKey, state.repo)
            is SnowbirdRepoViewModel.RepoState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun onJoinSuccess(group: SnowbirdGroup) {
        // Group name doesn't come back from backend by default so
        // we poke it in here.
        //
        group.name = groupName
        group.save()
        snowbirdRepoViewModel.createRepo(group.key, repoName)
    }

    private fun onLoading() {
        handleCreateGroupLoadingStatus(true)
    }

    private fun handleCreateGroupLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayCreateGroupManager.show(this@SnowbirdJoinGroupFragment)
        } else {
            FullScreenOverlayCreateGroupManager.hide()
        }
    }

    private fun onRepoCreated(groupKey: String, repo: SnowbirdRepo) {
        repo.permissions = "READ_WRITE"
        repo.groupKey = groupKey
        repo.save()
        handleCreateGroupLoadingStatus(false)
        snowbirdRepoViewModel.fetchRepos(groupKey, false)
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Success
            title = UiText.Resource(R.string.label_success_title)
            message = UiText.Dynamic("Successfully joined")
            positiveButton {
                text = UiText.Resource(R.string.label_got_it)
                action = {

                    val action = SnowbirdJoinGroupFragmentDirections.actionFragmentSnowbirdJoinGroupToFragmentSnowbirdSetupSuccess(
                        message = getString(R.string.you_have_successfully_joined_dweb),
                        dwebGroupKey = groupKey
                    )

                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setupSideEffects() {
        binding.repoNameTextfield.post {
            binding.repoNameTextfield.showKeyboard()
        }

        binding.btnNext.setOnClickListener {
            repoName = binding.repoNameTextfield.text?.toString().orEmpty()

            if (repoName.isBlank()) {
                binding.repoNameTextfield.error = "Repository name cannot be empty"
            } else {
                snowbirdGroupViewModel.joinGroup(uriString)
                dismissKeyboard(it)
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
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

    companion object {
        const val DWEB_GROUP_KEY = "dweb_group_key"
    }

    override fun getToolbarTitle(): String {
        return "Join Group"
    }
}