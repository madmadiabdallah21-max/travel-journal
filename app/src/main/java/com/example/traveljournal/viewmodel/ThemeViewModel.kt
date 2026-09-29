package com.example.traveljournal.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "theme_preferences")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

class ThemeViewModel(context: Context) : ViewModel() {
    private val dataStore = context.dataStore
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                dataStore.data.collect { preferences ->
                    _isDarkMode.value = preferences[DARK_MODE_KEY] ?: false
                }
            } catch (e: Exception) {
                // Fallback on error: default to false
                _isDarkMode.value = false
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    val current = preferences[DARK_MODE_KEY] ?: false
                    preferences[DARK_MODE_KEY] = !current
                }
            } catch (e: Exception) {
                // Silently fail if DataStore write fails; state won't persist but app won't crash
            }
        }
    }
}
