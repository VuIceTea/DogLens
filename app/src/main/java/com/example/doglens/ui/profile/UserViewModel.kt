package com.example.doglens.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


data class UserUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String? = null
)

class UserViewModel : ViewModel() {
    private val userRepository = AppContainer.userRepository
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = UserUiState(
                isLoading = false,
                error = "Không tìm thấy thông tin tài khoản"
            )
            return
        }
        viewModelScope.launch {
            userRepository.observeUser(uid)
                .catch { exeption ->
                    _uiState.value = UserUiState(
                        isLoading = false,
                        error = exeption.message ?: "Không thể tải thông tin người dùng."
                    )
                }.collect { user ->
                    _uiState.value = UserUiState(
                        isLoading = false,
                        user = user
                    )
                }
        }
    }
}