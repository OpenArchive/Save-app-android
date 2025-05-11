
package net.opendasharchive.openarchive.services.gdrive

import org.koin.androidx.viewmodel.dsl.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import org.koin.dsl.module

internal val gdriveModule = module {
    factory {
        GDriveClient(
            get(), GoogleAccountCredential.usingOAuth2(
                get(),
                GDriveApiConduit.SCOPE_NAMES.toList()
            ).apply {
                selectedAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
            })
    }
    factory { GDriveRepository(get()) }
    factory<GDriveConduit> { params -> GDriveConduit(params.get(), get()) }
    viewModel { GDriveViewModel() }
}