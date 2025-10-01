package net.opendasharchive.openarchive.util

// ProofMode - ENTIRE FILE COMMENTED OUT
// This file contains ProofMode-specific functionality that has been disabled

//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.security.keystore.UserNotAuthenticatedException
//import androidx.fragment.app.FragmentActivity
//import net.opendasharchive.openarchive.features.main.MainActivity
//import org.witness.proofmode.ProofMode
//import org.witness.proofmode.crypto.pgp.PgpUtils
//import org.witness.proofmode.service.MediaWatcher
//import timber.log.Timber
//import java.io.File
//
//object ProofModeHelper {
//
//    private var initialized = false
//
//    fun init(activity: FragmentActivity, completed: () -> Unit) {
//        if (initialized) return completed()
//
//        val encryptedPassphrase = Prefs.proofModeEncryptedPassphrase
//
//        if (encryptedPassphrase?.isNotEmpty() == true) {
//            // Sometimes this gets out of sync because of the restarts.
//            Prefs.useProofModeKeyEncryption = true
//
//            val key = Hbks.loadKey()
//
//            if (key != null) {
//                Hbks.decrypt(encryptedPassphrase, Hbks.loadKey(), activity) { plaintext, e ->
//                    // User failed or denied authentication. Stop app in that case.
//                    if (e is UserNotAuthenticatedException) {
//                        Runtime.getRuntime().exit(0)
//                    }
//                    else {
//                        finishInit(activity, completed, plaintext)
//                    }
//                }
//            }
//            else {
//                // Oh, oh. User removed passphrase lock.
//                Prefs.proofModeEncryptedPassphrase = null
//                Prefs.useProofModeKeyEncryption = false
//
//                removePgpKey(activity)
//
//                finishInit(activity, completed)
//            }
//        }
//        else {
//            // Sometimes this gets out of sync because of the restarts.
//            Prefs.useProofModeKeyEncryption = false
//
//            finishInit(activity, completed)
//        }
//    }
//
//    private fun finishInit(context: Context, completed: () -> Unit, passphrase: String? = null) {
//        try {
//            // Store unencrypted passphrase so MediaWatcher can read it.
//            Prefs.temporaryUnencryptedProofModePassphrase = passphrase
//
//            // Initialize ProofMode background service first
//            ProofMode.initBackgroundService(context)
//
//            // Initialize PGP utils with context and passphrase
//            PgpUtils.init(context, passphrase ?: Prefs.temporaryUnencryptedProofModePassphrase ?: "")
//
//            // Initialize MediaWatcher with the correct passphrase.
//            MediaWatcher.getInstance(context.applicationContext)
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to initialize ProofMode")
//        }
//
//        // Remove again to avoid leaking unencrypted passphrase.
//        Prefs.temporaryUnencryptedProofModePassphrase = null
//
//        initialized = true
//
//        completed()
//    }
//
//    fun removePgpKey(context: Context) {
//        for (file in arrayOf(File(context.filesDir, "pkr.asc"), File(context.filesDir, "pub.asc"))) {
//            try {
//                file.delete()
//            }
//            catch (e: Exception) {
//                Timber.d(e)
//            }
//        }
//    }
//
//    fun restartApp(activity: Activity) {
//        val i = Intent(activity, MainActivity::class.java)
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        activity.startActivity(i)
//
//        activity.finish()
//
//        Prefs.store()
//
//        Runtime.getRuntime().exit(0)
//    }
//}
