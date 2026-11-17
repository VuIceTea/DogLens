package com.example.doglens.data

import com.example.doglens.data.mock.MockDogRepository
import com.example.doglens.data.mock.MockFavoriteRepository
import com.example.doglens.data.mock.MockRecognitionRepository
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.RecognitionRepository

object AppContainer {
    // Có API: đổi MockDogRepository() -> ApiDogRepository(retrofitService)
    val dogRepository: DogRepository by lazy { MockDogRepository() }
    val favoriteRepository: FavoriteRepository by lazy { MockFavoriteRepository(dogRepository) }
    val recognitionRepository: RecognitionRepository by lazy { MockRecognitionRepository(shouldFail = false) }
}