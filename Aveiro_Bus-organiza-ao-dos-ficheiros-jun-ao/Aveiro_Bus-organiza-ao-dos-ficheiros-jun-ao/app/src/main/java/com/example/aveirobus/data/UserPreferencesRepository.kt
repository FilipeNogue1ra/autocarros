package com.example.aveirobus.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class UserPreferences(
    val darkModeEnabled: Boolean = false,
    val language: String = "pt", // Português por padrão
    val wheelchairAccessibilityEnabled: Boolean = false
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val WHEELCHAIR_ACCESSIBILITY = booleanPreferencesKey("wheelchair_accessibility")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val darkModeEnabled = preferences[PreferencesKeys.DARK_MODE] ?: false
            val language = preferences[PreferencesKeys.LANGUAGE] ?: "pt"
            val wheelchairAccessibilityEnabled =
                preferences[PreferencesKeys.WHEELCHAIR_ACCESSIBILITY] ?: false

            UserPreferences(
                darkModeEnabled = darkModeEnabled,
                language = language,
                wheelchairAccessibilityEnabled = wheelchairAccessibilityEnabled
            )
        }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun updateLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun updateWheelchairAccessibility(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WHEELCHAIR_ACCESSIBILITY] = enabled
        }
    }
}