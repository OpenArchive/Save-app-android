package net.opendasharchive.openarchive.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {

    val timezone = TimeZone.currentSystemDefault()

    val now: Long get() = Clock.System.now().toEpochMilliseconds()

    val nowDateTime: LocalDateTime get() = Clock.System.now().toLocalDateTime(timezone)
}

// Datetime Extension functions
fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(DateUtils.timezone)
}

fun Date.toKotlinLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this.time).toLocalDateTime(DateUtils.timezone)
}

fun LocalDateTime.toJavaDate(): Date {
    return Date(this.toInstant(DateUtils.timezone).toEpochMilliseconds())
}