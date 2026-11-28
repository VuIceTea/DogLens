package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirestoreCatalog(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun list(collection: String) =
        db.collection(collection).get().await().documents

    suspend fun byId(collection: String, id: String) =
        db.collection(collection).document(id).get().await().takeIf { it.exists() }

    suspend fun related(collection: String, field: String, id: String) =
        db.collection(collection).whereEqualTo(field, id).get().await().documents
}

internal fun DocumentSnapshot.string(field: String, fallback: String = "") =
    getString(field) ?: fallback

internal fun DocumentSnapshot.int(field: String): Int? = getLong(field)?.toInt()

internal fun DocumentSnapshot.imageResource(): Int? {
    if (!getString("imageUrl").isNullOrBlank()) return null
    return when (getString("imageKey")) {
        "corgi" -> R.drawable.corgi
        "explore_300_breeds" -> R.drawable.explore_300_breeds
        "dog_small_pomeranian" -> R.drawable.dog_small_pomeranian
        "french_bulldog" -> R.drawable.french_bulldog
        "golden_retriever" -> R.drawable.golden_retriever
        "group_guard_german_shepherd" -> R.drawable.group_guard_german_shepherd
        "group_herding_border_collie" -> R.drawable.group_herding_border_collie
        "group_hunting_golden" -> R.drawable.group_hunting_golden
        "group_sled_husky" -> R.drawable.group_sled_husky
        "img_golden_retriever" -> R.drawable.img_golden_retriever
        "poodle" -> R.drawable.poodle
        "siberian_husky" -> R.drawable.siberian_husky
        "topic_apartment_puppy" -> R.drawable.topic_apartment_puppy
        "topic_kid_friendly_golden" -> R.drawable.topic_kid_friendly_golden
        "topic_smart_border_collie" -> R.drawable.topic_smart_border_collie
        else -> null
    }
}

internal fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

internal fun DocumentSnapshot.sources(): Map<String, String> =
    (get("sources") as? Map<*, *>)?.entries.orEmpty().mapNotNull { (label, url) ->
        if (label is String && url is String && url.startsWith("https://")) label to url else null
    }.toMap()

internal fun String.cleanBreedCopy(): String = replace(" (danh mục FCI)", "")
    .replace(" trong danh mục FCI", "")
    .replace("Xuất xứ theo nguồn tham khảo: ", "Xuất xứ: ")
    .replace("FCI phân loại ba cỡ và ba kiểu lông.", "Giống này có ba cỡ và ba kiểu lông.")
    .trim()

internal fun DocumentSnapshot.toDog() = Dog(
    id = id,
    name = string("name"),
    description = (getString("catalogNote") ?: string("description")).cleanBreedCopy(),
    imageUrl = getString("imageUrl"),
    localImageRes = imageResource() ?: R.drawable.ic_detail_paw.takeIf {
        reference.parent.id == "dogs" && getString("imageUrl").isNullOrBlank()
    },
    backgroundColorHex = string("backgroundColorHex", "#E3F0FD"),
    sortOrder = int("sortOrder") ?: 0,
    sizeCategoryId = string("sizeCategoryId", "medium"),
    minWeightKg = int("minWeightKg"),
    maxWeightKg = int("maxWeightKg"),
    minHeightCm = int("minHeightCm"),
    maxHeightCm = int("maxHeightCm"),
    origin = getString("origin")?.cleanBreedCopy(),
    minLifespanYears = int("minLifespanYears"),
    maxLifespanYears = int("maxLifespanYears"),
    coatColors = getString("coatColors"),
    familyScore = int("familyScore"),
    kidsScore = int("kidsScore"),
    trainingScore = int("trainingScore"),
    apartmentScore = int("apartmentScore"),
    summary = (getString("catalogNote") ?: getString("summary"))?.cleanBreedCopy(),
    rating = (get("rating") as? Number)?.toDouble(),
    favoriteCount = int("favoriteCount"),
    exploreDogIds = stringList("dogIds"),
    lifespanMedianYears = getDouble("lifespanMedianYears"),
    heightStandard = getString("heightStandard"),
    weightStandard = getString("weightStandard"),
    weightMedianKg = getDouble("weightMedianKg"),
    weightReferenceKg = getDouble("weightReferenceKg"),
    lifespanReference = getString("lifespanReference"),
    sources = sources(),
    searchAliases = stringList("searchAliases"),
    longDescription = getString("longDescription")?.cleanBreedCopy()
)
