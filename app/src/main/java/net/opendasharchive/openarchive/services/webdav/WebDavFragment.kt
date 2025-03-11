package net.opendasharchive.openarchive.services.webdav

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.FragmentWebDavBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.internetarchive.Util
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class WebDavFragment : BaseFragment() {
    private var mSpaceId: Long? = null
    private lateinit var mSpace: Space

    private lateinit var mSnackbar: Snackbar
    private lateinit var binding: FragmentWebDavBinding

    private var originalName: String? = null
    private var isNameChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSpaceId = arguments?.getLong(ARG_SPACE_ID) ?: ARG_VAL_NEW_SPACE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWebDavBinding.inflate(inflater)

        mSpaceId = arguments?.getLong(ARG_SPACE_ID) ?: ARG_VAL_NEW_SPACE

        if (mSpaceId != ARG_VAL_NEW_SPACE) {
            // setup views for editing an existing space

            mSpace = Space.get(mSpaceId!!) ?: Space(Space.Type.WEBDAV)

            binding.header.visibility = View.GONE
            binding.buttonBar.visibility = View.GONE
            binding.buttonBarEdit.visibility = View.VISIBLE

            binding.server.isEnabled = false
            binding.username.isEnabled = false
            binding.password.isEnabled = false

            // Disable the password visibility toggle
            binding.passwordLayout.isEndIconVisible = false

            binding.server.setText(mSpace.host)
            binding.username.setText(mSpace.username)
            binding.password.setText(mSpace.password)

            binding.name.setText(mSpace.name)
            binding.layoutName.visibility = View.VISIBLE

//            mBinding.swChunking.isChecked = mSpace.useChunking
//            mBinding.swChunking.setOnCheckedChangeListener { _, useChunking ->
//                mSpace.useChunking = useChunking
//                mSpace.save()
//            }


            binding.btRemove.setOnClickListener {
                removeSpace()
            }

            // swap webDavFragment with Creative Commons License Fragment
//            binding.btLicense.setOnClickListener {
//                setFragmentResult(RESP_LICENSE, bundleOf())
//            }

//            binding.name.setOnEditorActionListener { _, actionId, _ ->
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//
//                    val enteredName = binding.name.text?.toString()?.trim()
//                    if (!enteredName.isNullOrEmpty()) {
//                        // Update the Space entity and save it using SugarORM
//                        mSpace.name = enteredName
//                        mSpace.save() // Save the entity using SugarORM
//
//                        // Hide the keyboard
//                        val imm =
//                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                        imm.hideSoftInputFromWindow(binding.name.windowToken, 0)
//                        binding.name.clearFocus() // Clear focus from the input field
//
//                        // Optional: Provide feedback to the user
//                        Snackbar.make(
//                            binding.root,
//                            "Name saved successfully!",
//                            Snackbar.LENGTH_SHORT
//                        ).show()
//                    } else {
//                        // Notify the user that the name cannot be empty (optional)
//                        Snackbar.make(binding.root, "Name cannot be empty", Snackbar.LENGTH_SHORT)
//                            .show()
//                    }
//
//                    true // Consume the event
//                } else {
//                    false // Pass the event to the next listener
//                }
//            }

            originalName = mSpace.name

            // Listen for name changes
            binding.name.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val enteredName = s?.toString()?.trim()
                    isNameChanged = enteredName != originalName
                    requireActivity().invalidateOptionsMenu() // Refresh menu to show confirm button
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            CreativeCommonsLicenseManager.initialize(binding.cc, mSpace.license) {
                mSpace.license = it
                mSpace.save()
            }

        } else {
            // setup views for creating a new space
            mSpace = Space(Space.Type.WEBDAV)
            binding.btRemove.visibility = View.GONE
            binding.buttonBar.visibility = View.VISIBLE
            binding.buttonBarEdit.visibility = View.GONE
            binding.layoutName.visibility = View.GONE
            binding.layoutLicense.visibility = View.GONE

            binding.btAuthenticate.isEnabled = false
            setupTextWatchers()

        }

        binding.btAuthenticate.setOnClickListener { attemptLogin() }

        binding.btCancel.setOnClickListener {
            if (isJetpackNavigation) {
                findNavController().popBackStack()
            } else {
                setFragmentResult(RESP_CANCEL, bundleOf())
            }
        }

        binding.server.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.server.setText(fixSpaceUrl(binding.server.text)?.toString())
            }
        }

        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                //attemptLogin()
            }

            false
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSnackbar = binding.root.makeSnackBar(getString(R.string.login_activity_logging_message))

        if (mSpaceId != ARG_VAL_NEW_SPACE) {
            val menuProvider = object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_confirm, menu)
                }

                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    val btnConfirm = menu.findItem(R.id.action_confirm)
                    btnConfirm?.isVisible = isNameChanged
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_confirm -> {
                            //todo: save changes here and show success dialog
                            saveChanges()
                            true
                        }
                        android.R.id.home -> {
                            if(isNameChanged) {
                                AppLogger.e("unsaved changes")
                                showUnsavedChangesDialog()
                                false
                            } else {
                                findNavController().popBackStack()
                            }
                        }
                        else -> false
                    }
                }
            }

            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.RESUMED
            )


            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (isNameChanged) {
                    showUnsavedChangesDialog()
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun saveChanges() {
        val enteredName = binding.name.text?.toString()?.trim()
        if (!enteredName.isNullOrEmpty()) {
            mSpace.name = enteredName
            mSpace.save()
            originalName = enteredName
            isNameChanged = false
            requireActivity().invalidateOptionsMenu() //Refresh menu to hide confirm btn again
            showSuccessDialog()
        } else {
            Snackbar.make(binding.root, "Name cannot be empty", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSuccessDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
           type = DialogType.Success
            title = R.string.label_success_title.asUiText()
            message = R.string.msg_edit_server_success.asUiText()
            icon = UiImage.DrawableResource(R.drawable.ic_done)
            positiveButton {
                text = UiText.StringResource(R.string.lbl_got_it)
                action = {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun showUnsavedChangesDialog() {
        dialogManager.showDialog(DialogConfig(
            type = DialogType.Warning,
            title = UiText.DynamicString("Unsaved changes!"),
            message = UiText.DynamicString("Do you want to save"),
            icon = UiImage.DynamicVector(Icons.Default.Warning),
            positiveButton = ButtonData(
                text = UiText.DynamicString("Save"),
                action = { saveChanges() }
            ),
            neutralButton = ButtonData(
                text = UiText.DynamicString("Discard"),
                action = { findNavController().popBackStack() }
            )
        ))
    }

    private fun fixSpaceUrl(url: CharSequence?): Uri? {
        if (url.isNullOrBlank()) return null

        val uri = Uri.parse(url.toString())
        val builder = uri.buildUpon()

        if (uri.scheme != "https") {
            builder.scheme("https")
        }

        if (uri.authority.isNullOrBlank()) {
            builder.authority(uri.path)
            builder.path(REMOTE_PHP_ADDRESS)
        } else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(REMOTE_PHP_ADDRESS)
        }

        return builder.build()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Reset errors.
        binding.username.error = null
        binding.password.error = null

        // Store values at the time of the login attempt.
        var errorView: View? = null

        mSpace.host = fixSpaceUrl(binding.server.text)?.toString() ?: ""
        binding.server.setText(mSpace.host)

        mSpace.username = binding.username.text?.toString() ?: ""
        mSpace.password = binding.password.text?.toString() ?: ""

        if (mSpace.host.isEmpty()) {
            binding.server.error = getString(R.string.error_field_required)
            errorView = binding.server
        } else if (mSpace.username.isEmpty()) {
            binding.username.error = getString(R.string.error_field_required)
            errorView = binding.username
        } else if (mSpace.password.isEmpty()) {
            binding.password.error = getString(R.string.error_field_required)
            errorView = binding.password
        }

        if (errorView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            errorView.requestFocus()

            return
        }

        val other = Space.get(Space.Type.WEBDAV, mSpace.host, mSpace.username)

        if (other.isNotEmpty() && other[0].id != mSpace.id) {
            return showError(getString(R.string.you_already_have_a_server_with_these_credentials))
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mSnackbar.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                testConnection()
                mSpace.save()
                Space.current = mSpace

//                CleanInsightsManager.getConsent(requireActivity()) {
//                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.WEBDAV.friendlyName)
//                }

                navigate(mSpace.id)
            } catch (exception: IOException) {
                if (exception.message?.startsWith("401") == true) {
                    showError(getString(R.string.error_incorrect_username_or_password), true)
                } else {
                    showError(exception.localizedMessage ?: getString(R.string.error))
                }
            }
        }
    }

    private fun navigate(spaceId: Long) = CoroutineScope(Dispatchers.Main).launch {
//        Utility.showMaterialMessage(
//            context = requireContext(),
//            title = "Success",
//            message = "You have successfully authenticated! Now let's continue setting up your media server."
//        ) {}
        if (isJetpackNavigation) {
            val action =
                WebDavFragmentDirections.actionFragmentWebDavToFragmentWebDavSetupLicense(
                    spaceId = spaceId
                )
            findNavController().navigate(action)
        } else {
            setFragmentResult(RESP_SAVED, bundleOf(ARG_SPACE_ID to spaceId))
        }

    }

    private suspend fun testConnection() {
        val url = mSpace.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(requireContext(), mSpace.username, mSpace.password)

        val request =
            Request.Builder().url(url).method("GET", null).addHeader("OCS-APIRequest", "true")
                .addHeader("Accept", "application/json").build()

        return suspendCoroutine {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val message = response.message

                    response.close()

                    if (code != 200 && code != 204) {
                        return it.resumeWith(Result.failure(IOException("$code $message")))
                    }

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        requireActivity().runOnUiThread {
            mSnackbar.dismiss()

            if (onForm) {
                binding.password.error = text
                binding.password.requestFocus()
            } else {
                mSnackbar = binding.root.makeSnackBar(text, Snackbar.LENGTH_LONG)
                mSnackbar.show()

                binding.server.requestFocus()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isNameChanged) {
            binding.name.requestFocus()
        }

        // make sure the snack-bar is gone when this fragment isn't on display anymore
        mSnackbar.dismiss()
        // also hide keyboard when fragment isn't on display anymore
        Util.hideSoftKeyboard(requireActivity())
    }

    private fun removeSpace() {
        val config = DialogConfig(
            type = DialogType.Warning,
            title = R.string.remove_from_app.asUiText(),
            message = R.string.are_you_sure_you_want_to_remove_this_server_from_the_app.asUiText(),
            icon = UiImage.DrawableResource(R.drawable.ic_trash),
            destructiveButton = ButtonData(
                text = UiText.StringResource(R.string.lbl_ok),
                action = {
                    mSpace.delete()
                    findNavController().popBackStack()
                }
            ),
            neutralButton = ButtonData(
                text = UiText.StringResource(R.string.lbl_Cancel),
                action = {}
            )
        )
        dialogManager.showDialog(config)
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

        binding.server.addTextChangedListener(textWatcher)
        binding.username.addTextChangedListener(textWatcher)
        binding.password.addTextChangedListener(textWatcher)
    }

    private fun updateAuthenticateButtonState() {
        val url = binding.server.text?.toString()?.trim().orEmpty()
        val username = binding.username.text?.toString()?.trim().orEmpty()
        val password = binding.password.text?.toString()?.trim().orEmpty()

        // Enable the button only if none of the fields are empty
        binding.btAuthenticate.isEnabled =
            url.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
    }

    companion object {
        // events emitted by this fragment
        const val RESP_SAVED = "web_dav_fragment_resp_saved"
        const val RESP_DELETED = "web_dav_fragment_resp_deleted"
        const val RESP_CANCEL = "web_dav_fragment_resp_cancel"
        const val RESP_LICENSE = "web_dav_fragment_resp_license"

        // factory method parameters (bundle args)
        const val ARG_SPACE_ID = "space_id"
        const val ARG_VAL_NEW_SPACE = -1L

        // other internal constants
        const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"

        @JvmStatic
        fun newInstance(spaceId: Long) = WebDavFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_SPACE_ID, spaceId)
            }
        }

        @JvmStatic
        fun newInstance() = newInstance(ARG_VAL_NEW_SPACE)
    }

    override fun getToolbarTitle(): String = if (mSpaceId == ARG_VAL_NEW_SPACE) {
        "Private Server"
    } else {
        val space = Space.get(mSpaceId!!)
        space?.name ?: "Private Server"
    }
}