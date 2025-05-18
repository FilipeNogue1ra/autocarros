package com.example.aveirobus

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.aveirobus.data.UserPreferencesRepository

class AveiroApplication : Application() {

    private val dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    val userPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }
}