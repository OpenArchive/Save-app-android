package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdJoinGroupBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.extensions.showKeyboard
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.util.FullScreenOverlayCreateGroupManager
import timber.log.Timber

class SnowbirdJoinGroupFragment: BaseFragment() {

    private lateinit var viewBinding: FragmentSnowbirdJoinGroupBinding
    private lateinit var uriString: String
    private lateinit var groupName: String
    private lateinit var repoName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            uriString = it.getString(ARG_RAVEN_GROUP_URI_STRING, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSnowbirdJoinGroupBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupName = uriString.getQueryParameter("name") ?: "Unknown group"

        Timber.d("uriString = $uriString")
        Timber.d("groupName = $groupName")

        viewBinding.groupNameTextfield.setText(groupName)

        setupViewModelObservers()
        setupSideEffects()
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
            title = UiText.StringResource(R.string.label_success_title)
            message = UiText.DynamicString("Successfully joined")
            positiveButton {
                text = UiText.StringResource(R.string.label_got_it)
                action = {
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun setupSideEffects() {
        viewBinding.repoNameTextfield.post {
            viewBinding.repoNameTextfield.showKeyboard()
        }

        viewBinding.joinGroupButton.setOnClickListener {
            repoName = viewBinding.repoNameTextfield.text?.toString().orEmpty()

            if (repoName.isBlank()) {
                viewBinding.repoNameTextfield.error = "Repository name cannot be empty"
            } else {
                snowbirdGroupViewModel.joinGroup(uriString)
                dismissKeyboard(it)
            }
        }
    }

    companion object {


        const val ARG_RAVEN_GROUP_URI_STRING = "space_setup_success_fragment_arg_message"

        @JvmStatic
        fun newInstance(uriString: String) =
            SnowbirdJoinGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RAVEN_GROUP_URI_STRING, uriString)
                }
            }
    }

    override fun getToolbarTitle(): String {
        return "Join Group"
    }
}