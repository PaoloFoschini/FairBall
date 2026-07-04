package com.example.fairball.data
// DataStoreManager.kt
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    val themePreferenceFlow: Flow<ThemePreference> = dataStore.data.map { preferences ->
        val value = preferences[THEME_KEY] ?: "system"
        when (value) {
            "light" -> ThemePreference.LIGHT
            "dark" -> ThemePreference.DARK
            "custom" -> ThemePreference.CUSTOM
            else -> ThemePreference.SYSTEM
        }
    }

    suspend fun saveThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = when (preference) {
                ThemePreference.LIGHT -> "light"
                ThemePreference.DARK -> "dark"
                ThemePreference.CUSTOM -> "custom"
                ThemePreference.SYSTEM -> "system"
            }
        }
    }
}