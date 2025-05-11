
package net.opendasharchive.openarchive.services.gdrive

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import org.koin.dsl.module

internal val gdriveModule = module {
    factory {
        GDriveClient(
            get(), GoogleAccountCredential.usingOAuth2(
                get(),
                setOf(DriveScopes.DRIVE_FILE, Scopes.EMAIL)
            ).apply {
                selectedAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
            })
    }
    single { GDriveRepository(get()) }
    factory<GDriveConduit> { params -> GDriveConduit(params.get(), get()) }
}