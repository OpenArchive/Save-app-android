package net.opendasharchive.openarchive.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {

    val timezone = TimeZone.currentSystemDefault()

    val now: Long get() = Clock.System.now().toEpochMilliseconds()

    val nowDateTime: LocalDateTime get() = Clock.System.now().toLocalDateTime(timezone)

    fun getTimestamp(): String = nowDateTime.format("yyyyMMdd_HHmmss", Locale.US)

    fun parseDateTime(value: String): LocalDateTime {
        return try {
            OffsetDateTime.parse(value).toLocalDateTime().toKotlinLocalDateTime()
        } catch (e: Exception) {
            nowDateTime
        }
    }
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

fun LocalDateTime.toEpochMilliseconds(): Long {
    return this.toInstant(DateUtils.timezone).toEpochMilliseconds()
}

fun LocalDateTime.format(pattern: String, locale: Locale = Locale.ENGLISH): String {
    return this.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern(pattern, locale))
}