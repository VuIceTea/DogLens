package com.example.doglens.data.repository

import com.example.doglens.data.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // Theo dõi hồ sơ người dùng liên tục
    fun observeUser(uid: String): Flow<User?>

    // Tạo hồ sơ lần đầu nếu chưa có
    suspend fun ensureUser(firebaseUser: FirebaseUser, registrationName: String? = null)

    // Cập nhật hồ sơ người dùng
    suspend fun updateUser(user: User)
}
