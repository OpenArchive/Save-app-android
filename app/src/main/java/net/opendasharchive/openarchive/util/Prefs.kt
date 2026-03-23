package net.opendasharchive.openarchive.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.preference.PreferenceManager

object Prefs {
    const val PASSCODE_ENABLED = "passcode_enabled"
    private const val DID_COMPLETE_ONBOARDING = "did_complete_onboarding"
    const val UPLOAD_WIFI_ONLY = "upload_wifi_only"
    private const val NEARBY_USE_BLUETOOTH = "nearby_use_bluetooth"
    private const val NEARBY_USE_WIFI = "nearby_use_wifi"
    const val USE_TOR = "pref_use_tor"
    const val PROHIBIT_SCREENSHOTS = "prohibit_screenshots"
    const val USE_C2PA = "use_c2pa"
    private const val C2PA_SIGNING_KEY = "c2pa_signing_key"
    private const val C2PA_ENCRYPTED_PASSPHRASE = "c2pa_encrypted_passphrase"
    // private const val USE_NEXTCLOUD_CHUNKING = "upload_nextcloud_chunks"
    const val THEME = "theme"
    private const val CURRENT_SPACE_ID = "current_space"
    private const val CURRENT_HOME_PAGE = "current_home_page"
    private const val FLAG_HINT_SHOWN = "ft.flag"
    private const val BATCH_HINT_SHOWN = "ft.batch"
    private const val ADD_MEDIA_HINT = "ft.addMedia"
    private const val DONT_SHOW_UPLOAD_HINT = "ft.upload"
    private const val IA_HINT_SHOWN = "ft.ia"
    private const val ADD_FOLDER_HINT_SHOWN = "ft.add_folder"
    private const val LICENSE_URL = "archive_pref_share_license_url"
    private const val DID_RUN_SEEDER = "did_run_seeder"
    private const val IS_MIGRATION_IN_PROGRESS = "is_migration_in_progress"
    private const val IS_ROOM_MIGRATED = "is_room_migrated"
    private const val STORACHA_WARNING_SHOWN = "storacha_warning_shown"

    val TOR_DOWNLOAD_URL = Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")

    private var prefs: SharedPreferences? = null

    fun load(context: Context) {
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    @SuppressLint("ApplySharedPref")
    fun store() {
        prefs?.edit()?.commit()
    }

    fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }

    fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs?.getInt(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return prefs?.getLong(key, defaultValue) ?: defaultValue
    }

    fun putLong(key: String, value: Long) {
        prefs?.edit()?.putLong(key, value)?.apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun putBooleanSync(key: String, value: Boolean) {
        // Use commit() for synchronous write - critical for values that must persist before activity recreation
        prefs?.edit()?.putBoolean(key, value)?.commit()
    }

//    val useNextcloudChunking: Boolean
//        get() = prefs?.getBoolean(USE_NEXTCLOUD_CHUNKING, false) ?: false

    var didCompleteOnboarding: Boolean
        get() = prefs?.getBoolean(DID_COMPLETE_ONBOARDING, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(DID_COMPLETE_ONBOARDING, value)?.apply()
        }

    var uploadWifiOnly: Boolean
        get() = prefs?.getBoolean(UPLOAD_WIFI_ONLY, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(UPLOAD_WIFI_ONLY, value)?.apply()
        }

    var nearbyUseBluetooth: Boolean
        get() = prefs?.getBoolean(NEARBY_USE_BLUETOOTH, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(NEARBY_USE_BLUETOOTH, value)?.apply()
        }

    var nearbyUseWifi: Boolean
        get() = prefs?.getBoolean(NEARBY_USE_WIFI, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(NEARBY_USE_WIFI, value)?.apply()
        }

    var useC2pa: Boolean
        get() = prefs?.getBoolean(USE_C2PA, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(USE_C2PA, value)?.apply()
        }

    var useTor: Boolean
        get() = prefs?.getBoolean(USE_TOR, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(USE_TOR, value)?.apply()
        }

    var currentSpaceId: Long
        get() = prefs?.getLong(CURRENT_SPACE_ID, -1) ?: -1
        set(value) {
            prefs?.edit()?.putLong(CURRENT_SPACE_ID, value)?.apply()
        }

    var currentHomePage: Int
        get() = prefs?.getInt(CURRENT_HOME_PAGE, 0) ?: 0
        set(value) {
            prefs?.edit()?.putInt(CURRENT_HOME_PAGE, value)?.apply()
        }

    var flagHintShown: Boolean
        get() = prefs?.getBoolean(FLAG_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(FLAG_HINT_SHOWN, value)?.apply()
        }

    var addMediaHint: Boolean
        get() = prefs?.getBoolean(ADD_MEDIA_HINT, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(ADD_MEDIA_HINT, value)?.apply()
        }

    var batchHintShown: Boolean
        get() = prefs?.getBoolean(BATCH_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(BATCH_HINT_SHOWN, value)?.apply()
        }

    var dontShowUploadHint: Boolean
        get() = prefs?.getBoolean(DONT_SHOW_UPLOAD_HINT, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(DONT_SHOW_UPLOAD_HINT, value)?.apply()
        }

    var iaHintShown: Boolean
        get() = prefs?.getBoolean(IA_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(IA_HINT_SHOWN, value)?.apply()
        }

    var addFolderHintShown: Boolean
        get() = prefs?.getBoolean(ADD_FOLDER_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(ADD_FOLDER_HINT_SHOWN, value)?.apply()
        }

    var licenseUrl: String?
        get() = prefs?.getString(LICENSE_URL, null)
        set(value) {
            prefs?.edit()?.putString(LICENSE_URL, value)?.apply()
        }

    var passcodeEnabled: Boolean
        get() = prefs?.getBoolean(PASSCODE_ENABLED, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(PASSCODE_ENABLED, value)?.apply()
        }

    var c2paSigningKey: String?
        get() = prefs?.getString(C2PA_SIGNING_KEY, null)
        set(value) {
            prefs?.edit()?.putString(C2PA_SIGNING_KEY, value)?.apply()
        }

    var c2paEncryptedPassphrase: ByteArray?
        get() {
            val passphrase = prefs?.getString(C2PA_ENCRYPTED_PASSPHRASE, null) ?: return null
            return Base64.decode(passphrase, Base64.DEFAULT)
        }
        set(value) {
            val passphrase = if (value == null) null else Base64.encodeToString(value, Base64.DEFAULT)
            prefs?.edit()?.putString(C2PA_ENCRYPTED_PASSPHRASE, passphrase)?.apply()
        }

    val theme: Theme
        get() = Theme.get(prefs?.getString(THEME, null))


    var prohibitScreenshots: Boolean
        get() = prefs?.getBoolean(PROHIBIT_SCREENSHOTS, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(PROHIBIT_SCREENSHOTS, value)?.apply()
        }

    var didRunSeeder: Boolean
        get() = prefs?.getBoolean(DID_RUN_SEEDER, false) ?: false
        set(value) {
            // Use commit() for synchronous write, crucial before app proceeds
            prefs?.edit()?.putBoolean(DID_RUN_SEEDER, value)?.commit()
        }

    var isMigrationInProgress: Boolean
        get() = prefs?.getBoolean(IS_MIGRATION_IN_PROGRESS, false) ?: false
        set(value) {
            putBoolean(IS_MIGRATION_IN_PROGRESS, value)
        }

    var isRoomMigrated: Boolean
        get() = prefs?.getBoolean(IS_ROOM_MIGRATED, false) ?: false
        set(value) {
            putBoolean(IS_ROOM_MIGRATED, value)
        }

    var storachaWarningShown: Boolean
        get() = prefs?.getBoolean(STORACHA_WARNING_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(STORACHA_WARNING_SHOWN, value)?.apply()
        }

}
