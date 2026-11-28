package com.example.doglens.data.repository

import com.example.doglens.data.model.FavoriteDog
import com.example.doglens.data.model.FavoriteDogItem
import com.example.doglens.utils.TimeUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

internal class FirestoreFavoriteRepository(
    private val dogs: DogRepository,
    private val account: FirestoreAccountData = FirestoreAccountData()
) : FavoriteRepository {
    override suspend fun getFavoriteDogIds(): Set<String> {
        val favorites = account.collection("favorites") ?: return emptySet()
        val snapshot = withTimeoutOrNull(3_000) { favorites.get().await() } ?: favorites.get(Source.CACHE).await()
        return snapshot.documents.map { it.id }.toSet()
    }

    override suspend fun getFavoriteItems(): List<FavoriteDogItem> {
        val favorites = account.collection("favorites") ?: return emptyList()
        val query = favorites.orderBy("addedAt")
        val snapshot = withTimeoutOrNull(3_000) { query.get().await() } ?: query.get(Source.CACHE).await()
        val documents = snapshot.documents
        if (documents.isEmpty()) return emptyList()
        val dogsById = dogs.getDogs().associateBy { it.id }
        val uid = checkNotNull(favorites.parent).id
        return documents.mapNotNull { document ->
            val dog = dogsById[document.id] ?: return@mapNotNull null
            val addedAt = document.getTimestamp("addedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: 0L
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
