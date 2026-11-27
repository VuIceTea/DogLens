package com.example.doglens.data.repository

import com.example.doglens.data.model.CareTip
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogCareTip
import com.example.doglens.data.model.DogImage
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait

interface DogRepository {
    suspend fun getDogs(): List<Dog>
    suspend fun getPopularDogs(): List<Dog> = getDogs().take(5)
    suspend fun getDogById(id: String): Dog?
    fun cachedDog(id: String): Dog? = null
    suspend fun getDogImages(dogId: String): List<DogImage>
    suspend fun getTraits(): List<Trait>
    suspend fun getDogTraits(dogId: String): List<DogTrait>
    suspend fun getDogTraitsByDog(dogIds: Set<String>): Map<String, List<DogTrait>> =
        dogIds.associateWith { getDogTraits(it) }
    suspend fun getCareTips(): List<CareTip>
    suspend fun getDogCareTips(dogId: String): List<DogCareTip>
    suspend fun getSizeCategories(): List<DogSizeCategory>
}
