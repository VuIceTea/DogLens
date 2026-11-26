package com.example.doglens.ui.auth

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class PendingEmailVerification(val email: String, val sent: Boolean)

data class EmailAuthState(
    val loading: Boolean = false,
    val completed: Boolean = false,
    val message: String? = null,
    val emailAlreadyRegistered: Boolean = false,
    val passwordError: String? = null,
    val verification: PendingEmailVerification? = null
)

class EmailAuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val mutableState = MutableStateFlow(EmailAuthState())
    val state = mutableState.asStateFlow()
    private var lastVerificationEmail: String? = null
    private var lastVerificationTime = 0L

    fun signIn(email: String, password: String) = authenticate(registering = false, email = email) {
        auth.signInWithEmailAndPassword(email, password).await().user
    }

    fun register(name: String, email: String, password: String) = authenticate(email = email) {
        auth.createUserWithEmailAndPassword(email, password).await().user?.also { user ->
            try {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A profile update failure should not prevent verification.
            }
        }
    }

    private fun authenticate(
        registering: Boolean = true,
        email: String,
        action: suspend () -> FirebaseUser?
    ) {
        if (mutableState.value.loading || mutableState.value.completed || mutableState.value.verification != null) return
        mutableState.value = EmailAuthState(loading = true)
        viewModelScope.launch {
            val user = try {
                withContext(NonCancellable) {
                    val account = checkNotNull(action())
                    if (!account.isEmailVerified) {
                        val address = account.email ?: email
                        val sent = try {
                            sendVerification(account, address)
                        } finally {
                            if (auth.currentUser?.uid == account.uid) auth.signOut()
                        }
                        mutableState.value = EmailAuthState(verification = PendingEmailVerification(address, sent))
                        return@withContext null
                    }
                    account
                } ?: return@launch
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (registering && error is FirebaseAuthUserCollisionException) {
                    mutableState.value = EmailAuthState(emailAlreadyRegistered = true)
                    return@launch
                }
                if (error is FirebaseAuthWeakPasswordException) {
                    mutableState.value = EmailAuthState(passwordError = "Mật khẩu chưa đáp ứng yêu cầu bảo mật.")
                    return@launch
                }
                if (!registering && (error is FirebaseAuthInvalidCredentialsException || error is FirebaseAuthInvalidUserException)) {
                    mutableState.value = EmailAuthState(passwordError = "Email hoặc mật khẩu không đúng.")
                    return@launch
                }
                val message = when (error) {
                    is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng thử lại."
                    is FirebaseTooManyRequestsException -> "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau."
                    else -> if (registering) "Không thể đăng ký. Vui lòng thử lại."
                        else "Không thể đăng nhập. Hãy kiểm tra email và mật khẩu."
                }
                mutableState.value = EmailAuthState(message = message)
                return@launch
            }

            val message = try {
                AppContainer.userRepository.ensureUser(user)
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                "Đã đăng nhập. Chưa thể đồng bộ hồ sơ, vui lòng thử lại sau."
            }
            mutableState.value = EmailAuthState(completed = true, message = message)
        }
    }

    private suspend fun sendVerification(account: FirebaseUser, email: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (lastVerificationEmail.equals(email, ignoreCase = true) && now - lastVerificationTime < 60_000) {
            return true
        }
        return try {
            auth.setLanguageCode("vi")
            account.sendEmailVerification().await()
            lastVerificationEmail = email
            lastVerificationTime = SystemClock.elapsedRealtime()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    fun consumeOutcome() {
        mutableState.value = EmailAuthState()
    }
}
