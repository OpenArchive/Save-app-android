package net.opendasharchive.openarchive.db

import androidx.room3.ColumnTypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import kotlin.time.Instant

class Converters {
    @ColumnTypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    @ColumnTypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
    }

    @ColumnTypeConverter
    fun fromVaultType(value: VaultType): Int = when (value) {
        VaultType.PRIVATE_SERVER -> 0
        VaultType.INTERNET_ARCHIVE -> 1
        VaultType.DWEB_STORAGE -> 5
    }

    @ColumnTypeConverter
    fun toVaultType(value: Int): VaultType = when (value) {
        0 -> VaultType.PRIVATE_SERVER
        1 -> VaultType.INTERNET_ARCHIVE
        5 -> VaultType.DWEB_STORAGE
        else -> VaultType.PRIVATE_SERVER
    }

    @ColumnTypeConverter
    fun fromEvidenceStatus(value: EvidenceStatus): Int = value.id

    @ColumnTypeConverter
    fun toEvidenceStatus(value: Int): EvidenceStatus = when (value) {
        0 -> EvidenceStatus.NEW
        1 -> EvidenceStatus.LOCAL
        2 -> EvidenceStatus.QUEUED
        4 -> EvidenceStatus.UPLOADING
        5 -> EvidenceStatus.UPLOADED
        9 -> EvidenceStatus.ERROR
        else -> EvidenceStatus.NEW
    }
}
