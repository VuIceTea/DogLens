package com.example.doglens.data.repository

import com.example.doglens.data.model.FavoriteDog
import com.example.doglens.data.model.FavoriteDogItem
import com.example.doglens.utils.TimeUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class FirestoreFavoriteRepository(
    private val dogs: DogRepository,
    private val account: FirestoreAccountData = FirestoreAccountData()
) : FavoriteRepository {
    override suspend fun getFavoriteItems(): List<FavoriteDogItem> {
        val favorites = account.collection("favorites") ?: return emptyList()
        val documents = favorites.orderBy("addedAt").get().await().documents
        if (documents.isEmpty()) return emptyList()
        val dogsById = dogs.getDogs().associateBy { it.id }
        val uid = checkNotNull(favorites.parent).id
        return documents.mapNotNull { document ->
            val dog = dogsById[document.id] ?: return@mapNotNull null
            val addedAt = document.getTimestamp("addedAt")?.toDate()?.time ?: 0L
            FavoriteDogItem(dog, FavoriteDog(document.id, uid, dog.id, TimeUtils.toIso(addedAt)))
        }
    }

    override suspend fun isFavorite(dogId: String): Boolean =
        account.collection("favorites")?.document(dogId)?.get()?.await()?.exists() == true

    override fun observeFavorite(dogId: String) = callbackFlow {
        val reference = account.collection("favorites")?.document(dogId)
        if (reference == null) {
            trySend(FavoriteStatus(false))
            close()
            return@callbackFlow
        }
        val listener = reference.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) close(error)
            else if (snapshot != null) trySend(FavoriteStatus(snapshot.exists(), snapshot.metadata.hasPendingWrites()))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun addFavorite(dogId: String) {
        account.requireCollection("favorites").document(dogId)
            .set(mapOf("addedAt" to FieldValue.serverTimestamp())).await()
    }

    override suspend fun removeFavorite(dogId: String) {
        account.requireCollection("favorites").document(dogId).delete().await()
    }
}
