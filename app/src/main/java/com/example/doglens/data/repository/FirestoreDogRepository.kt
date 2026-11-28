package com.example.doglens.data.repository

import android.os.SystemClock
import com.example.doglens.data.model.CareTip
import com.example.doglens.data.model.DogCareTip
import com.example.doglens.data.model.DogImage
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait
import com.example.doglens.data.model.Dog
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

internal class FirestoreDogRepository(
    private val catalog: FirestoreCatalog = FirestoreCatalog()
) : DogRepository {
    private val dogsById = ConcurrentHashMap<String, Dog>()
    @Volatile private var dogCatalog: Recent<List<Dog>>? = null
    @Volatile private var traitCatalog: Recent<Map<String, List<DogTrait>>>? = null
    @Volatile private var traitLabels: Recent<List<Trait>>? = null

    private data class Recent<T>(val data: T, val loadedAt: Long = SystemClock.elapsedRealtime()) {
        fun current(): T? = data.takeIf { SystemClock.elapsedRealtime() - loadedAt < 60_000L }
    }

    override fun cachedDog(id: String): Dog? = dogsById[id]

    override fun invalidateCatalog() {
        dogCatalog = null
        traitCatalog = null
        traitLabels = null
    }

    private fun DocumentSnapshot.readDog() = toDog().also { dogsById[it.id] = it }

    override suspend fun getDogs(): List<Dog> =
        dogCatalog?.current() ?: catalog.list("dogs").map { it.readDog() }.sortedBy { it.sortOrder }
            .also { dogCatalog = Recent(it) }

    override suspend fun getPopularDogs() =
        catalog.db.collection("dogs").orderBy("sortOrder").limit(5).get().await().documents.map { it.readDog() }

    override suspend fun getDogById(id: String): Dog? {
        val document = catalog.byId("dogs", id)
        if (document == null) {
            dogsById.remove(id)
            dogCatalog = null
            return null
        }
        return document.readDog()
    }

    override suspend fun getDogImages(dogId: String) =
        catalog.related("dogImages", "dogId", dogId).filter { it.getBoolean("editorialVerified") != false }.map {
            DogImage(it.id, dogId, it.getString("imageUrl"), it.imageResource(), it.int("sortOrder") ?: 0)
        }.sortedBy { it.sortOrder }

    override suspend fun getTraits(): List<Trait> = traitLabels?.current() ?: catalog.list("traits")
        .map { Trait(it.id, it.string("name"), it.getString("iconKey"), it.string("colorHex", "#E3F0FD")) }
        .also { traitLabels = Recent(it) }

    override suspend fun getDogTraits(dogId: String) =
        traitCatalog?.current()?.let { it[dogId].orEmpty() }
            ?: catalog.related("dogTraits", "dogId", dogId).readDogTraits()

    override suspend fun getDogTraitsByDog(dogIds: Set<String>): Map<String, List<DogTrait>> {
        if (dogIds.isEmpty()) return emptyMap()
        val byDog = traitCatalog?.current() ?: catalog.list("dogTraits").readDogTraits()
            .groupBy { it.dogId }
            .also { traitCatalog = Recent(it) }
        return byDog.filterKeys { it in dogIds }
    }

    private fun List<DocumentSnapshot>.readDogTraits() =
        filter { it.getBoolean("editorialVerified") != false }
            .sortedBy { it.int("sortOrder") ?: 0 }
            .map { DogTrait(it.string("dogId"), it.string("traitId"), it.getString("description")) }

    override suspend fun getCareTips() = catalog.list("careTips").map {
        CareTip(it.id, it.string("name"), it.string("iconKey"), it.string("colorHex", "#E8F2FE"))
    }

    override suspend fun getDogCareTips(dogId: String) =
        catalog.related("dogCareTips", "dogId", dogId).filter { it.getBoolean("editorialVerified") != false }
            .sortedBy { it.int("sortOrder") ?: 0 }.map {
            DogCareTip(dogId, it.string("careTipId"), it.getString("description"))
        }

    override suspend fun getSizeCategories() = catalog.list("dogSizeCategories").map {
        DogSizeCategory(it.id, it.string("name"), it.int("sortOrder") ?: 0)
    }.sortedBy { it.sortOrder }
}
