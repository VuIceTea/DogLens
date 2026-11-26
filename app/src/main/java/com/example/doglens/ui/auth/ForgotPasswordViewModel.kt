package com.example.doglens.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val resendSeconds: Int = 0,
    val message: String? = null
)

class ForgotPasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun send(rawEmail: String) {
        if (_state.value.isLoading || _state.value.sent) return
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
        requestReset(email, resend = false)
    }

    fun resend() {
        val current = _state.value
        if (!current.sent || current.isLoading || current.resendSeconds > 0) return
        requestReset(current.email, resend = true)
    }

    private fun requestReset(email: String, resend: Boolean) {
        _state.value = _state.value.copy(email = email, emailError = null, isLoading = true, message = null)
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            val error = task.exception
            if (task.isSuccessful || error is FirebaseAuthInvalidUserException) {
                _state.value = _state.value.copy(
                    sent = true,
                    isLoading = false,
                    resendSeconds = 30,
                    message = if (resend) "Đã xử lý yêu cầu gửi lại. Hãy kiểm tra hộp thư." else null
                )
                viewModelScope.launch {
                    for (remaining in 29 downTo 0) {
                        delay(1_000)
                        _state.value = _state.value.copy(resendSeconds = remaining)
                    }
                }
            } else {
                val message = when (error) {
                    is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng thử lại."
                    is FirebaseTooManyRequestsException -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
                    else -> "Không thể xử lý yêu cầu đặt lại mật khẩu. Vui lòng thử lại."
                }
                _state.value = _state.value.copy(isLoading = false, message = message)
            }
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
