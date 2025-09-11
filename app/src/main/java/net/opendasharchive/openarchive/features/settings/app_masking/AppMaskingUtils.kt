package net.opendasharchive.openarchive.features.settings.app_masking

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit

object AppMaskingUtils {

    private const val PREFS_NAME = "app_masking_prefs"
    private const val KEY_ENABLED_ALIAS = "key_enabled_alias"

    /**
     * Call this to enable a specific activity-alias and disable all others.
     * @param context Application or activity context
     * @param aliasToEnable The fully qualified name of the alias to enable
     */
    fun setLauncherActivityAlias(context: Context, aliasToEnable: String) {
        val packageName = context.packageName
        val pm = context.packageManager

        // List the aliases you care about (two in your case: Save vs. Mask).
        val allAliases = listOf(
            "$packageName.alias.SaveAlias",
            "$packageName.alias.MaskAlias"
        )

        // For each known alias, enable the chosen one, disable the rest
        allAliases.forEach { alias ->
            val newState =
                if (alias == aliasToEnable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            pm.setComponentEnabledSetting(
                ComponentName(packageName, alias),
                newState,
                PackageManager.DONT_KILL_APP
            )
        }

        // Persist the chosen alias in SharedPreferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_ENABLED_ALIAS, aliasToEnable)
        }
    }

    /**
     * Returns the currently enabled alias, if any, as a fully-qualified name.
     */
    fun getCurrentAlias(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENABLED_ALIAS, null)
    }

    /**
     * A convenience method to translate the stored fully-qualified alias
     * into something human-readable for display (optional).
     */
    fun getCurrentAliasDisplayName(context: Context): String {
        return when (getCurrentAlias(context)) {
            "${context.packageName}.alias.SaveAlias" -> "Save App (default)"
            "${context.packageName}.alias.MaskAlias" -> "Masked App"
            else -> "Unknown / Default"
        }
    }
}