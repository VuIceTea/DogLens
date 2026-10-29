package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.FavoriteDog
import com.example.doglens.data.model.FavoriteDogItem

class FavoriteRepository(
    private val dogRepository: DogRepository = DogRepository()
) {

    companion object {
        const val CURRENT_USER_ID = "user_001"
    }

    private val favorites = mutableListOf(
        FavoriteDog(
            "favorite_001", CURRENT_USER_ID,
            "golden_retriever", "2026-10-28T09:30:00"
        ),
        FavoriteDog(
            "favorite_002", CURRENT_USER_ID,
            "corgi", "2026-10-27T15:20:00"
        ),
        FavoriteDog(
            "favorite_003", CURRENT_USER_ID,
            "siberian_husky", "2026-10-27T14:00:00"
        ),
        FavoriteDog(
            "favorite_004", CURRENT_USER_ID,
            "poodle", "2026-10-26T10:00:00"
        ),
        FavoriteDog(
            "favorite_005", CURRENT_USER_ID,
            "french_bulldog", "2026-10-25T08:00:00"
        ),
        FavoriteDog(
            "favorite_006", CURRENT_USER_ID,
            "pomeranian", "2026-10-24T12:00:00"
        )
    )

    fun getSizeCategories(): List<DogSizeCategory> = listOf(
        DogSizeCategory("small", "Chó nhỏ", 1),
        DogSizeCategory("medium", "Chó trung", 2),
        DogSizeCategory("large", "Chó lớn", 3)
    )


    fun getFavoriteItems(userId: String = CURRENT_USER_ID): List<FavoriteDogItem> {
        val dogsById = dogRepository.getDogs().associateBy { it.id }

        return favorites.filter { it.userId == userId }
            .mapNotNull { favorite ->
                val dog = dogsById[favorite.dogId] ?: return@mapNotNull null
                FavoriteDogItem(dog, favorite)
            }.sortedBy { it.favorite.addedAt }
    }

    fun removeFavorite(dogId: String, userId: String = CURRENT_USER_ID) {
        favorites.removeAll {
            it.userId == userId && it.dogId == dogId
        }
    }
}