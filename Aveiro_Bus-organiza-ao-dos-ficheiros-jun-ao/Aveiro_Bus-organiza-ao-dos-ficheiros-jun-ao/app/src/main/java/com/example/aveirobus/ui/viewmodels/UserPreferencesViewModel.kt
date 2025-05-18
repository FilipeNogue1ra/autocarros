package com.example.aveirobus.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aveirobus.data.UserPreferences
import com.example.aveirobus.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserPreferencesViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    val userPreferencesFlow: StateFlow<UserPreferences?> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(enabled)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            repository.updateLanguage(language)
        }
    }

    fun updateWheelchairAccessibility(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateWheelchairAccessibility(enabled)
        }
    }
}

class UserPreferencesViewModelFactory(
    private val repository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserPreferencesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserPreferencesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}