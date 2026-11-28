package com.example.doglens.data.repository

import com.example.doglens.data.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun refreshUser(uid: String): User? =
        usersCollection.document(uid).get(Source.SERVER).await().toObject(User::class.java)

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
            // The server owns premiumUntil and adds it only after payment.
            document.set(newUser.toProfileDocument()).await()
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
        usersCollection.document(user.id).update(
            mapOf(
                "fullName" to user.fullName,
                "phoneNumber" to user.phoneNumber,
                "birthDate" to user.birthDate,
                "gender" to user.gender,
                "address" to user.address,
                "customAvatarUrl" to user.customAvatarUrl,
                "hasCustomAvatar" to user.hasCustomAvatar
            )
        ).await()
    }

    private fun User.toProfileDocument(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "membership" to membership,
        "authProvider" to authProvider,
        "createdAt" to createdAt
    )

}
