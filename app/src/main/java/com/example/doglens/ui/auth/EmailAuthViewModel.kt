package com.example.doglens.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EmailAuthState(
    val loading: Boolean = false,
    val completed: Boolean = false,
    val message: String? = null
)

class EmailAuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val mutableState = MutableStateFlow(EmailAuthState())
    val state = mutableState.asStateFlow()

    fun signIn(email: String, password: String) = authenticate(registering = false) {
        auth.signInWithEmailAndPassword(email, password).await().user
    }

    fun register(name: String, email: String, password: String) = authenticate(registrationName = name) {
        auth.createUserWithEmailAndPassword(email, password).await().user?.also { user ->
            try {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the new account usable and attempt to save its name in Firestore.
            }
        }
    }

    private fun authenticate(
        registering: Boolean = true,
        registrationName: String? = null,
        action: suspend () -> FirebaseUser?
    ) {
        if (mutableState.value.loading || mutableState.value.completed) return
        mutableState.value = EmailAuthState(loading = true)
        viewModelScope.launch {
            val user = try {
                checkNotNull(action())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = when (error) {
                    is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng thử lại."
                    is FirebaseTooManyRequestsException -> "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau."
                    is FirebaseAuthWeakPasswordException -> "Mật khẩu chưa đáp ứng yêu cầu bảo mật."
                    is FirebaseAuthUserCollisionException -> "Không thể tạo tài khoản với email này. Hãy thử đăng nhập."
                    else -> if (registering) "Không thể đăng ký. Vui lòng thử lại."
                        else "Không thể đăng nhập. Hãy kiểm tra email và mật khẩu."
                }
                mutableState.value = EmailAuthState(message = message)
                return@launch
            }

            val message = try {
                AppContainer.userRepository.ensureUser(user, registrationName)
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                "Đã đăng nhập. Chưa thể đồng bộ hồ sơ, vui lòng thử lại sau."
            }
            mutableState.value = EmailAuthState(completed = true, message = message)
        }
    }

    fun consumeOutcome() {
        mutableState.value = EmailAuthState()
    }
}
