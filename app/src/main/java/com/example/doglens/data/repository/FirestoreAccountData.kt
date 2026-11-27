package com.example.doglens.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

internal class FirestoreAccountData(
    val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun collection(name: String) =
        auth.currentUser?.uid?.let { db.collection("users").document(it).collection(name) }

    fun requireCollection(name: String) =
        collection(name) ?: error("Vui lòng đăng nhập để sử dụng chức năng này.")

    fun observeCount(uid: String, name: String) = callbackFlow<Int?> {
        var receivedServerCount = false
        val registration = db.collection("users").document(uid).collection(name)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(null)
                } else if (snapshot != null) {
                    receivedServerCount = receivedServerCount || !snapshot.metadata.isFromCache
                    val available = receivedServerCount || !snapshot.isEmpty || snapshot.metadata.hasPendingWrites()
                    trySend(snapshot.size().takeIf { available })
                }
            }
        awaitClose { registration.remove() }
    }
}
