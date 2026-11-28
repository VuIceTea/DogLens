package com.example.doglens.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


data class UserUiState(
    val user: User? = null,
    val error: String? = null
)

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = AppContainer.userRepository
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    private var observedUid: String? = null
    private var profileJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener { loadUser() }

    init {
        loadUser()
        auth.addAuthStateListener(authListener)
    }

    suspend fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        val current = userRepository.refreshUser(uid)
        if (auth.currentUser?.uid == uid) {
            if (current != null) _uiState.value = UserUiState(user = current)
            if (profileJob?.isActive != true) {
                observedUid = null
                loadUser()
            }
        }
    }

    private fun loadUser() {
        val account = auth.currentUser
        if (observedUid == account?.uid) return

        observedUid = account?.uid
        profileJob?.cancel()
        if (account == null) {
            _uiState.value = UserUiState()
            return
        }

        val fallback = User(
            id = account.uid,
            fullName = account.displayName.orEmpty().ifBlank { account.email.orEmpty() },
            email = account.email.orEmpty(),
            avatarUrl = account.photoUrl?.toString()
        )
        _uiState.value = UserUiState(user = fallback)
        profileJob = viewModelScope.launch {
            userRepository.observeUser(account.uid)
                .catch { exception ->
                    _uiState.value = UserUiState(
                        user = fallback,
                        error = exception.message ?: "Không thể tải thông tin người dùng."
                    )
                }.collect { user ->
                    _uiState.value = UserUiState(user = user ?: fallback)
                }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
