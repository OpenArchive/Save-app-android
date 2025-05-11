package net.opendasharchive.openarchive.services.gdrive

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Space

const val RESULT_SIGN_IN = 1000
const val REQUEST_CODE_GOOGLE_AUTH = 1001
const val PERMISSION_REQUEST_CODE = 1002

class GDriveViewModel : ViewModel() {

    fun authenticate(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_CLIENT_ID.apps.googleusercontent.com")
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE), *GDriveApiConduit.SCOPES)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)

        startActivityForResult(activity, googleSignInClient.signInIntent, RESULT_SIGN_IN, null)
    }

    private fun authorize(activity: Activity) =
        if (!GDriveConduit.permissionsGranted(activity)) {
            GoogleSignIn.requestPermissions(
                activity,
                REQUEST_CODE_GOOGLE_AUTH,
                GoogleSignIn.getLastSignedInAccount(activity),
                *GDriveConduit.SCOPES
            )
            true
        } else {
            false
        }

    private fun requestPermissions(activity: Activity): Boolean =
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.GET_ACCOUNTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.GET_ACCOUNTS),
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }

    fun saveSpace(context: Context, callback: () -> Unit) {
        viewModelScope.launch {
            val space = Space(Space.Type.GDRIVE)
            // we don't really know the host here, that's hidden by Drive Api
            space.host = GDriveApiConduit.NAME
            val account = GoogleSignIn.getLastSignedInAccount(context)
            space.displayname = account?.email ?: ""
            space.save()
            Space.current = space
            MainScope().launch { callback() }
        }
    }
}