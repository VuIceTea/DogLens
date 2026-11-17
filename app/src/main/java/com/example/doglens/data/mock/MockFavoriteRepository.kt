package com.example.doglens.data.mock

import com.example.doglens.data.model.FavoriteDog
import com.example.doglens.data.model.FavoriteDogItem
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.utils.TimeUtils

class MockFavoriteRepository(
    private val dogRepository: DogRepository
) : FavoriteRepository {

    // user đang đăng nhập (mock) -> lấy từ dữ liệu Profile có sẵn
    private val userId = ProfileMockData.profile.user.id

    private val favorites = mutableListOf(
        FavoriteDog("favorite_001", userId, "golden_retriever", "2026-10-28T09:30:00"),
        FavoriteDog("favorite_002", userId, "corgi", "2026-10-27T15:20:00"),
        FavoriteDog("favorite_003", userId, "siberian_husky", "2026-10-27T14:00:00"),
        FavoriteDog("favorite_004", userId, "poodle", "2026-10-26T10:00:00"),
        FavoriteDog("favorite_005", userId, "french_bulldog", "2026-10-25T08:00:00"),
        FavoriteDog("favorite_006", userId, "pomeranian", "2026-10-24T12:00:00")
    )

    override suspend fun getFavoriteItems(): List<FavoriteDogItem> {
        val dogsById = dogRepository.getDogs().associateBy { it.id }
        return favorites.filter { it.userId == userId }.mapNotNull { favorite ->
            val dog = dogsById[favorite.dogId] ?: return@mapNotNull null
            FavoriteDogItem(dog, favorite)
        }.sortedBy { it.favorite.addedAt }
    }

    override suspend fun isFavorite(dogId: String): Boolean =
        favorites.any { it.userId == userId && it.dogId == dogId }

    override suspend fun addFavorite(dogId: String) {
        if (isFavorite(dogId)) return
        favorites.add(
            FavoriteDog("favorite_${System.currentTimeMillis()}", userId, dogId, TimeUtils.nowIso())
        )
    }

    override suspend fun removeFavorite(dogId: String) {
        favorites.removeAll { it.userId == userId && it.dogId == dogId }
    }
}