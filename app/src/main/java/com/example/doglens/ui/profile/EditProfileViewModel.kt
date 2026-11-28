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

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val user: User? = null,
    val isSaved: Boolean = false,
    val error: String? = null
)

class EditProfileViewModel : ViewModel() {
    private val userRepository = AppContainer.userRepository

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    suspend fun refresh() {
        if (_uiState.value.isSaving) return
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val user = userRepository.refreshUser(uid)
        if (auth.currentUser?.uid == uid && !_uiState.value.isSaving) {
            _uiState.value = _uiState.value.copy(
                isLoading = false, user = user, error = if (user == null) "Không tìm thấy hồ sơ người dùng." else null
            )
        }
    }

    // Lấy thông tin người dùng hiện tại
    private fun loadUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = EditProfileUiState(
                isLoading = false,
                error = "Không tìm thấy tài khoản đăng nhập"
            )
            return
        }
        viewModelScope.launch {
            userRepository.observeUser(uid)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Không tìm thấy thông tin người dùng."
                    )
                }.collect { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        error = if (user == null) {
                            "Không tìm thấy hồ sơ người dùng."
                        } else {
                            null
                        }
                    )
                }
        }
    }

    fun updateProfile(
        fullName: String,
        phoneNumber: String,
        birthDate: String,
        gender: String,
        address: String
    ) {
        val currentUser = _uiState.value.user

        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                error = "Chưa tải được thông tin người dùng."
            )
            return
        }
        if (fullName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Vui lòng nhập họ và tên."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                isSaved = false,
                error = null
            )
            try {
                val updatedUser = currentUser.copy(
                    fullName = fullName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    birthDate = birthDate.trim(),
                    gender = gender.trim(),
                    address = address.trim()
                )
                userRepository.updateUser(updatedUser)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    user = updatedUser,
                    isSaved = true,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaved = false,
                    error = exception.message ?: "Không thể cập nhật thông tin."
                )
            }
        }
    }

    // Xóa trạng thái thông báo lưu thành công
    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(
            isSaved = false
        )
    }
}
