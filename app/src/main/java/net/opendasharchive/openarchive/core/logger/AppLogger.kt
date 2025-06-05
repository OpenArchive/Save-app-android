package net.opendasharchive.openarchive.core.logger

import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.DiskLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.util.Analytics
import timber.log.Timber


/**
 * A utility object for centralized logging in Android applications.
 * This object simplifies the logging process by integrating with the Timber and
 * AndroidRemoteDebugger libraries.
 *
 * Logs will only be generated if the [init] method is called. The class supports
 * different log levels and allows for conditional remote debugging.
 * The name of the class from which AppLogger was called will automatically be set as a tag
 */
object AppLogger {

    /**
     * Initializes the logger by planting a Timber DebugTree and optionally
     * initializing the AndroidRemoteDebugger.
     *
     * @param context The context used to initialize the AndroidRemoteDebugger.
     * @param initDebugger A boolean flag to determine whether AndroidRemoteDebugger
     *                     should be initialized.
     */
    fun init(context: Context, initDebugger: Boolean) {

        Timber.plant(DebugTreeWithTag())
    }

    // Info Level Logging
    fun i(message: String, vararg args: Any?) {
        Timber.i(message + args.joinToString(" "))
        Analytics.log(Analytics.APP_LOG, mapOf("info" to message + args.joinToString(" ")))
    }

    fun i(message: String, throwable: Throwable) {
        Timber.i(throwable, message)
        Analytics.log(Analytics.APP_LOG, mapOf("info" to message))
    }

    // Debug Level Logging
    fun d(message: String, vararg args: Any?) {
        Timber.d(message + args.joinToString(" "))
    }

    fun d(message: String, throwable: Throwable) {
        Timber.d(throwable, message)
    }

    // Error Level Logging
    fun e(message: String, vararg args: Any?) {
        Timber.e(message + args.joinToString(" "))
        Analytics.log(Analytics.APP_ERROR, mapOf("error" to message + args.joinToString(" ")))
    }

    fun e(message: String, throwable: Throwable) {
        Timber.e(throwable, message)
        Analytics.log(Analytics.APP_ERROR, mapOf("error" to message))
    }

    fun e(throwable: Throwable) {
        Timber.e(throwable)
        Analytics.log(Analytics.APP_ERROR, mapOf("error" to throwable.message))
    }

    // Warning Level Logging
    fun w(message: String, vararg args: Any?) {
        Timber.w(message + args.joinToString(" "))
    }

    fun w(message: String, throwable: Throwable) {
        Timber.w(throwable, message)
    }

    // Verbose Level Logging
    fun v(message: String, vararg args: Any?) {
        Timber.v(message + args.joinToString(" "))
    }

    // Tagged Logging Methods
    fun tagD(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).d("%s%s", message, args.joinToString(" "))
    }

    fun tagI(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).i("%s%s", message, args.joinToString(" "))
    }

    fun tagE(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).e("%s%s", message, args.joinToString(" "))
    }

    private class DebugTreeWithTag : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String? {
            // Customize the tag to include the class name and line number
            return "${element.fileName}:${element.lineNumber}"
        }
    }
}