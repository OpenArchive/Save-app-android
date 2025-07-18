package net.opendasharchive.openarchive.util.extensions

import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getVersionName(packageName: String): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }
        else {
            getPackageInfo(packageName, 0)
        }.versionName

    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
