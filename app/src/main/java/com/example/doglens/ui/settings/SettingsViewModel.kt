package com.example.doglens.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val darkMode: Boolean = false,
    val recognitionNotifications: Boolean = true,
    val articleNotifications: Boolean = true,
    val deletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    val deletionMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("doglens_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(
            darkMode = preferences.getBoolean("dark_mode", false),
            recognitionNotifications = preferences.getBoolean("recognition_notifications", true),
            articleNotifications = preferences.getBoolean("article_notifications", true)
        )
    }

    fun setDarkMode(value: Boolean) = updatePreference("dark_mode", value)
    fun setRecognitionNotifications(value: Boolean) = updatePreference("recognition_notifications", value)
    fun setArticleNotifications(value: Boolean) = updatePreference("article_notifications", value)

    private fun updatePreference(key: String, value: Boolean) {
        if (preferences.contains(key) && preferences.getBoolean(key, value) == value) return
        preferences.edit().putBoolean(key, value).apply()
        refresh()
    }

    fun deleteAccount(confirmedUid: String) {
        if (_state.value.deletingAccount || _state.value.accountDeleted) return
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.uid != confirmedUid) {
            _state.value = _state.value.copy(deletionMessage = "Phiên đăng nhập đã thay đổi. Vui lòng thử lại.")
            return
        }

        _state.value = _state.value.copy(deletingAccount = true, deletionMessage = null)
        viewModelScope.launch {
            try {
                user.delete().await()
                _state.value = _state.value.copy(deletingAccount = false, accountDeleted = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = when (error) {
                    is FirebaseAuthRecentLoginRequiredException ->
                        "Vui lòng đăng xuất, đăng nhập lại rồi quay về Cài đặt để xác nhận xóa."
                    is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng thử lại."
                    else -> "Không thể xóa tài khoản. Vui lòng thử lại."
                }
                _state.value = _state.value.copy(deletingAccount = false, deletionMessage = message)
            }
        }
    }

    fun consumeDeletionOutcome() {
        _state.value = _state.value.copy(accountDeleted = false, deletionMessage = null)
    }
}
