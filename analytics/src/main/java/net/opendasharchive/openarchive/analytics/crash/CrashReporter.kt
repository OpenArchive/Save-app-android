package net.opendasharchive.openarchive.analytics.crash

interface CrashReporter {
    fun recordException(throwable: Throwable)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
}
