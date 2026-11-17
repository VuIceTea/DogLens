package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.FavoriteDog
import com.example.doglens.data.model.FavoriteDogItem

interface FavoriteRepository {
    suspend fun getFavoriteItems(): List<FavoriteDogItem>
    suspend fun isFavorite(dogId: String): Boolean
    suspend fun addFavorite(dogId: String)
    suspend fun removeFavorite(dogId: String)
}