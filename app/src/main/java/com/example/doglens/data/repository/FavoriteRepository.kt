package com.example.doglens.data.repository

import com.example.doglens.data.model.FavoriteDogItem
import kotlinx.coroutines.flow.flow

data class FavoriteStatus(val saved: Boolean, val pending: Boolean = false)

interface FavoriteRepository {
    suspend fun getFavoriteItems(): List<FavoriteDogItem>
    suspend fun isFavorite(dogId: String): Boolean
    fun observeFavorite(dogId: String) = flow { emit(FavoriteStatus(isFavorite(dogId))) }
    suspend fun addFavorite(dogId: String)
    suspend fun removeFavorite(dogId: String)
}
