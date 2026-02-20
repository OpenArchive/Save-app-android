package net.opendasharchive.openarchive.core.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    fun observeCurrentSpaceId(): Flow<Long>
    suspend fun setCurrentSpaceId(id: Long)
}

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val CURRENT_SPACE_ID = longPreferencesKey("current_space_id")
    }

    override fun observeCurrentSpaceId(): Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_SPACE_ID] ?: -1L
        }

    override suspend fun setCurrentSpaceId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_SPACE_ID] = id
        }
    }
}
