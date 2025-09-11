package net.opendasharchive.openarchive.util

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Available themes.
 *
 * Available options need to case-insensitively match the constants used in
 * `R.array.ar_prefs_theme_val`, which should be:
 * - `R.string.prefs_theme_val_system`
 * - `R.string.prefs_theme_val_light`
 * - `R.string.prefs_theme_val_dark`
 */
enum class Theme(val mode: Int) {

    /**
     * Follow the system-wide setting.
     */
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),

    /**
     * Force light ("non-night-mode") theme.
     */
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),

    /**
     * Force dark ("night mode") theme.
     */
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {

        /**
         * Set the used theme. Defaults to `SYSTEM`, when argument is `null`.
         *
         * @param theme: The `Theme` to set.
         */
        fun set(context: Context, theme: Theme?) {
            val useDarkMode = theme == DARK
            AppCompatDelegate.setDefaultNightMode((theme ?: SYSTEM).mode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val uiModeManager = context.getSystemService(UI_MODE_SERVICE) as UiModeManager
                val darkMode = if (useDarkMode) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
                uiModeManager.setApplicationNightMode(darkMode)
            } else {
                val darkMode = if (useDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(darkMode)
            }
        }

        /**
         * Get the `Theme` with the given name. Defaults to `SYSTEM` on `null` or no match.
         *
         * @param name: Theme name, case insensitive.
         */
        fun get(name: String?): Theme {
            return entries.firstOrNull { it.name.uppercase() == name?.uppercase() } ?: SYSTEM
        }

        var darkModeEnabled: Boolean
            get() {
                return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            }
            set(value) {
                AppCompatDelegate.setDefaultNightMode(if (value) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            }
    }
}
