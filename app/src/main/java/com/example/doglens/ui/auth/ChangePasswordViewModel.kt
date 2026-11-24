package com.example.doglens.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChangePasswordUiState(
    val isSocialAccount: Boolean = false,
    val isLoading: Boolean = false,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val message: String? = null,
    val success: Boolean = false
)

class ChangePasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(ChangePasswordUiState(isSocialAccount = !usesPasswordProvider()))
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    private fun usesPasswordProvider(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    fun updatePassword(current: String, newPassword: String, confirmation: String) {
        if (_state.value.isSocialAccount || _state.value.isLoading) return
        val currentError = if (current.isBlank()) "Vui lòng nhập mật khẩu hiện tại" else null
        val newError = when {
            newPassword.isBlank() -> "Vui lòng nhập mật khẩu mới"
            newPassword.length < 8 -> "Mật khẩu phải có ít nhất 8 ký tự"
            !newPassword.any(Char::isUpperCase) -> "Cần ít nhất một chữ cái viết hoa"
            !newPassword.any(Char::isDigit) -> "Cần ít nhất một chữ số"
            newPassword == current -> "Mật khẩu mới phải khác mật khẩu hiện tại"
            else -> null
        }
        val confirmError = when {
            confirmation.isBlank() -> "Vui lòng xác nhận mật khẩu mới"
            confirmation != newPassword -> "Mật khẩu xác nhận không khớp"
            else -> null
        }
        if (currentError != null || newError != null || confirmError != null) {
            _state.value = _state.value.copy(currentPasswordError = currentError, newPasswordError = newError, confirmPasswordError = confirmError)
            return
        }

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            _state.value = _state.value.copy(message = "Không tìm thấy tài khoản đang đăng nhập")
            return
        }
        _state.value = _state.value.copy(isLoading = true, currentPasswordError = null, newPasswordError = null, confirmPasswordError = null, message = null)
        user.reauthenticate(EmailAuthProvider.getCredential(email, current))
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { _state.value = _state.value.copy(isLoading = false, success = true, message = "Đổi mật khẩu thành công") }
                    .addOnFailureListener { _state.value = _state.value.copy(isLoading = false, message = it.localizedMessage ?: "Không thể cập nhật mật khẩu") }
            }
            .addOnFailureListener {
                _state.value = _state.value.copy(isLoading = false, currentPasswordError = "Mật khẩu hiện tại không chính xác")
            }
    }

    fun clearFieldErrors() {
        _state.value = _state.value.copy(currentPasswordError = null, newPasswordError = null, confirmPasswordError = null)
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null, success = false) }
}
