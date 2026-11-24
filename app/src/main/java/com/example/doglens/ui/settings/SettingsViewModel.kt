package com.example.doglens.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val darkMode: Boolean = false,
    val recognitionNotifications: Boolean = true,
    val articleNotifications: Boolean = true
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    fun setDarkMode(value: Boolean) { _state.value = _state.value.copy(darkMode = value) }
    fun setRecognitionNotifications(value: Boolean) { _state.value = _state.value.copy(recognitionNotifications = value) }
    fun setArticleNotifications(value: Boolean) { _state.value = _state.value.copy(articleNotifications = value) }
}
