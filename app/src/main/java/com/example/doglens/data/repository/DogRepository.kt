package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait

interface DogRepository {
    suspend fun getDogs(): List<Dog>
    suspend fun getDogById(id: String): Dog?
    suspend fun getTraits(): List<Trait>
    suspend fun getDogTraits(dogId: String): List<DogTrait>
    suspend fun getSizeCategories(): List<DogSizeCategory>
}