package net.opendasharchive.openarchive.features.settings.app_masking

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import net.opendasharchive.openarchive.R

enum class AppMaskId {
    DEFAULT,
    CALCULATOR,
    DICTIONARY,
    CALENDAR
}

data class AppMask(
    val id: AppMaskId,
    val alias: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int
)

object AppMaskingUtils {

    private const val PREFS_NAME = "app_masking_prefs"
    private const val KEY_ENABLED_ALIAS = "key_enabled_alias"

    private const val DEFAULT_ALIAS_SUFFIX = "SaveAlias"
    private const val CALCULATOR_ALIAS_SUFFIX = "MaskAlias"
    private const val DICTIONARY_ALIAS_SUFFIX = "DictionaryAlias"
    private const val CALENDAR_ALIAS_SUFFIX = "CalendarAlias"

    fun getMaskOptions(context: Context): List<AppMask> {
        val packageName = context.packageName
        return listOf(
            AppMask(
                id = AppMaskId.DEFAULT,
                alias = packageName.alias(DEFAULT_ALIAS_SUFFIX),
                titleRes = R.string.app_mask_default_label,
                descriptionRes = R.string.app_mask_default_description,
                iconRes = R.drawable.ic_mask_save_icon
            ),
            AppMask(
                id = AppMaskId.CALCULATOR,
                alias = packageName.alias(CALCULATOR_ALIAS_SUFFIX),
                titleRes = R.string.app_mask_calculator_label,
                descriptionRes = R.string.app_mask_calculator_description,
                iconRes = R.drawable.ic_mask_save_calculator
            ),
            AppMask(
                id = AppMaskId.DICTIONARY,
                alias = packageName.alias(DICTIONARY_ALIAS_SUFFIX),
                titleRes = R.string.app_mask_dictionary_label,
                descriptionRes = R.string.app_mask_dictionary_description,
                iconRes = R.drawable.ic_mask_save_dictionary
            ),
            AppMask(
                id = AppMaskId.CALENDAR,
                alias = packageName.alias(CALENDAR_ALIAS_SUFFIX),
                titleRes = R.string.app_mask_calendar_label,
                descriptionRes = R.string.app_mask_calendar_description,
                iconRes = R.drawable.ic_mask_save_calendar
            )
        )
    }

    fun getCurrentAlias(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(
                KEY_ENABLED_ALIAS,
                context.packageName.alias(DEFAULT_ALIAS_SUFFIX)
            ) ?: context.packageName.alias(DEFAULT_ALIAS_SUFFIX)
    }

    fun getCurrentMask(context: Context): AppMask {
        val currentAlias = getCurrentAlias(context)
        return getMaskOptions(context).firstOrNull { it.alias == currentAlias }
            ?: getMaskOptions(context).first()
    }

    fun getCurrentAliasDisplayName(context: Context): String {
        val mask = getCurrentMask(context)
        return context.getString(mask.titleRes)
    }

    fun setLauncherActivityAlias(context: Context, mask: AppMask): Result<Unit> {
        return try {
            val pm = context.packageManager
            val allAliases = getMaskOptions(context).map { it.alias }.distinct()

            allAliases.forEach { alias ->
                val newState =
                    if (alias == mask.alias) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    }
                val componentName = ComponentName(context, alias)
                pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_ENABLED_ALIAS, mask.alias)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String.alias(suffix: String): String = "$this.alias.$suffix"
}
