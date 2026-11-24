package com.example.doglens.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val message: String? = null
)

class ForgotPasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun send(rawEmail: String) {
        if (_state.value.isLoading) return
        val email = rawEmail.trim()
        val error = when {
            email.isBlank() -> "Vui lòng nhập email"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không đúng định dạng"
            else -> null
        }
        if (error != null) {
            _state.value = _state.value.copy(emailError = error)
            return
        }
        _state.value = _state.value.copy(email = email, emailError = null, isLoading = true)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { _state.value = _state.value.copy(isLoading = false, sent = true) }
            .addOnFailureListener { _state.value = _state.value.copy(isLoading = false, message = it.localizedMessage ?: "Không thể gửi email đặt lại mật khẩu") }
    }

    fun resend() {
        val email = _state.value.email
        if (email.isNotBlank()) {
            _state.value = _state.value.copy(isLoading = true)
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { _state.value = _state.value.copy(isLoading = false, message = "Đã gửi lại liên kết") }
                .addOnFailureListener { _state.value = _state.value.copy(isLoading = false, message = it.localizedMessage ?: "Không thể gửi lại email") }
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
