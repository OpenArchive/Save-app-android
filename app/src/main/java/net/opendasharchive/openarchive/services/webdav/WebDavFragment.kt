package net.opendasharchive.openarchive.services.webdav

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine
import androidx.core.net.toUri
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import com.google.android.material.textfield.TextInputLayout

class WebDavFragment : BaseFragment() {

    private lateinit var mSpace: Space

    private var isLoading = false
    private lateinit var binding: FragmentWebDavBinding

    private var originalName: String? = null
    private var isNameChanged = false

    private var usernameEndIconModeBeforeError: Int? = null
    private var usernameEndIconDrawableBeforeError: Drawable? = null
    private var usernameEndIconVisibleBeforeError: Boolean = false
    private var passwordEndIconModeBeforeError: Int? = null
    private var passwordEndIconDrawableBeforeError: Drawable? = null
    private var passwordEndIconVisibleBeforeError: Boolean = false

    private val args: WebDavFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWebDavBinding.inflate(inflater)

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->

            bottomMargin = insets.bottom
        }

        if (args.spaceId != ARG_VAL_NEW_SPACE) {
            // setup views for editing an existing space

            mSpace = Space.get(args.spaceId) ?: Space(Space.Type.WEBDAV)

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

                override fun afterTextChanged(s: Editable?) {}
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
            findNavController().popBackStack()
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

        if (args.spaceId != ARG_VAL_NEW_SPACE) {
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
        val enteredName = binding.name.text?.toString()?.trim().orEmpty()

        mSpace.name = enteredName
        mSpace.save()
        originalName = enteredName
        isNameChanged = false
        requireActivity().invalidateOptionsMenu() //Refresh menu to hide confirm btn again
        showSuccessDialog()
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
            title = UiText.StringResource(R.string.unsaved_changes),
            message = UiText.StringResource(R.string.do_you_want_to_save),
            icon = UiImage.DynamicVector(Icons.Default.Warning),
            positiveButton = ButtonData(
                text = UiText.StringResource(R.string.lbl_save),
                action = { saveChanges() }
            ),
            neutralButton = ButtonData(
                text = UiText.StringResource(R.string.lbl_discard),
                action = { findNavController().popBackStack() }
            )
        ))
    }

    private fun fixSpaceUrl(url: CharSequence?): Uri? {
        if (url.isNullOrBlank()) return null

        val uri = url.toString().toUri()
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

        // Show loading overlay and make screen non-interactable
        showLoadingOverlay(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                testConnection()
                mSpace.save()
                Space.current = mSpace

//                CleanInsightsManager.getConsent(requireActivity()) {
//                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.WEBDAV.friendlyName)
//                }

                // Hide loading overlay on success and navigate
                requireActivity().runOnUiThread {
                    showLoadingOverlay(false)
                }
                navigate(mSpace.id)
            } catch (exception: IOException) {
                when {
                    exception.message?.startsWith("401") == true -> {
                        showInvalidCredentialsError()
                    }
                    exception.message?.contains("Unable to resolve host", ignoreCase = true) == true -> {
                        showError("A server with the specified hostname could not be found")
                    }
                    else -> {
                        showError(exception.localizedMessage ?: getString(R.string.error))
                    }
                }
            }
        }
    }

    private fun showInvalidCredentialsError() {
        requireActivity().runOnUiThread {
            showLoadingOverlay(false)
            binding.errorHint.text = getString(R.string.error_incorrect_username_or_password)
            binding.errorHint.show()
            binding.username.error = null
            binding.usernameLayout.error = null
            binding.password.error = null
            binding.passwordLayout.error = null

            if (usernameEndIconModeBeforeError == null) {
                usernameEndIconModeBeforeError = binding.usernameLayout.endIconMode
                usernameEndIconDrawableBeforeError = binding.usernameLayout.endIconDrawable
                usernameEndIconVisibleBeforeError = binding.usernameLayout.isEndIconVisible
            }

            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)?.let { drawable ->
                binding.usernameLayout.apply {
                    endIconMode = TextInputLayout.END_ICON_CUSTOM
                    endIconDrawable = drawable
                    isEndIconVisible = true
                    setEndIconTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorError
                            )
                        )
                    )
                    endIconContentDescription =
                        getString(R.string.error_incorrect_username_or_password)
                }
            }

            if (passwordEndIconModeBeforeError == null) {
                passwordEndIconModeBeforeError = binding.passwordLayout.endIconMode
                passwordEndIconDrawableBeforeError = binding.passwordLayout.endIconDrawable
                passwordEndIconVisibleBeforeError = binding.passwordLayout.isEndIconVisible
            }

            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)?.let { drawable ->
                binding.passwordLayout.apply {
                    endIconMode = TextInputLayout.END_ICON_CUSTOM
                    endIconDrawable = drawable
                    isEndIconVisible = true
                    setEndIconTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorError
                            )
                        )
                    )
                    endIconContentDescription =
                        getString(R.string.error_incorrect_username_or_password)
                }
            }
        }
    }

    private fun dismissCredentialsError() {
        binding.errorHint.hide()
        // Clear error states from TextFields
        binding.username.error = null
        binding.usernameLayout.error = null
        binding.password.error = null
        binding.passwordLayout.error = null

        if (usernameEndIconModeBeforeError != null ||
            usernameEndIconDrawableBeforeError != null ||
            usernameEndIconVisibleBeforeError
        ) {
            binding.usernameLayout.apply {
                val modeToRestore = usernameEndIconModeBeforeError ?: TextInputLayout.END_ICON_NONE
                endIconMode = modeToRestore
                setEndIconDrawable(usernameEndIconDrawableBeforeError)
                isEndIconVisible = usernameEndIconVisibleBeforeError
                setEndIconTintList(null)
                endIconContentDescription = null
            }
        }

        if (passwordEndIconModeBeforeError != null ||
            passwordEndIconDrawableBeforeError != null ||
            passwordEndIconVisibleBeforeError
        ) {
            binding.passwordLayout.apply {
                val modeToRestore =
                    passwordEndIconModeBeforeError ?: TextInputLayout.END_ICON_PASSWORD_TOGGLE
                endIconMode = modeToRestore
                val drawableToRestore = passwordEndIconDrawableBeforeError
                    ?: AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.password_toggle_icon
                    )
                setEndIconDrawable(drawableToRestore)
                isEndIconVisible = passwordEndIconVisibleBeforeError
                setEndIconTintList(null)
                endIconContentDescription = null
            }
        }

        usernameEndIconModeBeforeError = null
        usernameEndIconDrawableBeforeError = null
        usernameEndIconVisibleBeforeError = false
        passwordEndIconModeBeforeError = null
        passwordEndIconDrawableBeforeError = null
        passwordEndIconVisibleBeforeError = false
    }

    private fun showLoadingOverlay(show: Boolean) {
        isLoading = show
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            // Disable all interactive elements during loading
            binding.server.isEnabled = false
            binding.username.isEnabled = false
            binding.password.isEnabled = false
            binding.btAuthenticate.isEnabled = false
            binding.btCancel.isEnabled = false
            if (args.spaceId != ARG_VAL_NEW_SPACE) {
                binding.btRemove.isEnabled = false
            }
        } else {
            // Re-enable elements based on original state
            if (args.spaceId != ARG_VAL_NEW_SPACE) {
                // For existing spaces, keep server/username/password disabled
                binding.server.isEnabled = false
                binding.username.isEnabled = false
                binding.password.isEnabled = false
                binding.btRemove.isEnabled = true
            } else {
                // For new spaces, enable all fields
                binding.server.isEnabled = true
                binding.username.isEnabled = true
                binding.password.isEnabled = true
                // Update authenticate button state based on form content
                updateAuthenticateButtonState()
            }
            binding.btCancel.isEnabled = true
        }
    }

    private fun navigate(spaceId: Long) = CoroutineScope(Dispatchers.Main).launch {
            val action =
                WebDavFragmentDirections.actionFragmentWebDavToFragmentSetupLicense(
                    spaceId = spaceId,
                    spaceType = Space.Type.WEBDAV
                )
            findNavController().navigate(action)
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
            showLoadingOverlay(false)

            if (onForm) {
                binding.errorHint.text = text
                binding.errorHint.show()
                binding.password.requestFocus()
            } else {
                // Show error dialog for server errors
                dialogManager.showErrorDialog(
                    message = text.toString(),
                    title = getString(R.string.error),
                    onDismiss = { binding.server.requestFocus() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isNameChanged) {
            binding.name.requestFocus()
        }

        // Hide loading overlay when fragment isn't on display anymore
        showLoadingOverlay(false)
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
                text = UiText.StringResource(R.string.lbl_remove),
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
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAuthenticateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {
                dismissCredentialsError()
            }
        }

        binding.server.addTextChangedListener(textWatcher)
        binding.username.addTextChangedListener(textWatcher)
        binding.password.addTextChangedListener(textWatcher)
    }

    private fun updateAuthenticateButtonState() {
        // Don't update button state if loading
        if (isLoading) return

        val url = binding.server.text?.toString()?.trim().orEmpty()
        val username = binding.username.text?.toString()?.trim().orEmpty()
        val password = binding.password.text?.toString()?.trim().orEmpty()

        // Enable the button only if none of the fields are empty and not loading
        binding.btAuthenticate.isEnabled =
            url.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
    }

    companion object {
        const val ARG_VAL_NEW_SPACE = -1L

        // other internal constants
        const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    override fun getToolbarTitle(): String = if (args.spaceId == ARG_VAL_NEW_SPACE) {
        "Private Server"
    } else {
        val space = Space.get(args.spaceId)
        when {
            space?.name?.isNotBlank() == true -> space.name
            space?.friendlyName?.isNotBlank() == true -> space.friendlyName
            else -> "Private Server"
        }
    }
}
