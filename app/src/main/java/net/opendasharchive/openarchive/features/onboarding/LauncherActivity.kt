package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.Prefs

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Check if onboarding is complete
        if (Prefs.didCompleteOnboarding) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, Onboarding23Activity::class.java))
        }
        finish() // Remove LauncherActivity from back stack
    }
}
