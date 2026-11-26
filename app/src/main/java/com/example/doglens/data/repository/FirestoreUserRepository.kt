package com.example.doglens.data.repository

import com.example.doglens.data.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCollection
            .document(uid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    // Có lỗi -> đóng Flow và báo lỗi
                    close(exception)
                    return@addSnapshotListener
                }
                // Nếu có lỗi (mạng, permission...), thì đóng Flow và ném exception ra
                if (snapshot == null || !snapshot.exists()) {
                    // Không tìm thấy user → gửi null
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot.toObject(User::class.java))
            }
        awaitClose {
            listener.remove()
        }
    }

    override suspend fun ensureUser(firebaseUser: FirebaseUser, registrationName: String?) {
        val uid = firebaseUser.uid
        val document = usersCollection.document(uid)
        val snapshot = document.get().await()

        val provider =
            firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId
                ?: "email"

        if (!snapshot.exists()) {
            val newUser = User(
                id = uid,
                fullName = registrationName ?: firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                membership = "FREE",
                authProvider = provider,
                createdAt = System.currentTimeMillis().toString()
            )
            document.set(newUser).await()
        } else {
            // Chỉ đồng bộ thông tin tài khoản từ Firebase Auth.
            // Không ghi đè tên và avatar riêng người dùng đã chỉnh.
            document.update(
                mapOf(
                    "email" to (firebaseUser.email ?: ""),
                    "authProvider" to provider
                )
            ).await()
        }
    }

    override suspend fun updateUser(user: User) {
        require(user.id.isNotBlank()) {
            "User ID không được để trống"
        }
        usersCollection.document(user.id).set(user).await()
    }

}
