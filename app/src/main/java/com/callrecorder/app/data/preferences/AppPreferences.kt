package com.callrecorder.app.data.preferences

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "call_recorder_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_RECORDING_ENABLED = booleanPreferencesKey("is_recording_enabled")
        private val KEY_STORAGE_PATH = stringPreferencesKey("custom_storage_path")
        private val KEY_DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")

        fun getDefaultStorageDirectory(): String {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val callRecordingsDir = File(musicDir, "CallRecordings")
            if (!callRecordingsDir.exists()) {
                callRecordingsDir.mkdirs()
            }
            return callRecordingsDir.absolutePath
        }
    }

    val isRecordingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RECORDING_ENABLED] ?: true
    }

    val storagePath: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STORAGE_PATH] ?: getDefaultStorageDirectory()
    }

    val isDisclaimerAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISCLAIMER_ACCEPTED] ?: false
    }

    suspend fun setRecordingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RECORDING_ENABLED] = enabled
        }
    }

    suspend fun setStoragePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORAGE_PATH] = path
        }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISCLAIMER_ACCEPTED] = accepted
        }
    }
}
