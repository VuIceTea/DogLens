package com.example.doglens.data

import com.example.doglens.data.mock.MockDogRepository
import com.example.doglens.data.mock.MockFavoriteRepository
import com.example.doglens.data.mock.MockRecognitionRepository
import com.example.doglens.data.mock.MockArticleRepository
import com.example.doglens.data.repository.ArticleRepository
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.FirestoreUserRepository
import com.example.doglens.data.repository.RecognitionRepository
import com.example.doglens.data.repository.UserRepository

object AppContainer {
    // Có API: đổi MockDogRepository() -> ApiDogRepository(retrofitService)
    val dogRepository: DogRepository by lazy { MockDogRepository() }
    val favoriteRepository: FavoriteRepository by lazy { MockFavoriteRepository(dogRepository) }
    val recognitionRepository: RecognitionRepository by lazy { MockRecognitionRepository(shouldFail = false) }
    val articleRepository: ArticleRepository by lazy { MockArticleRepository() }
    val userRepository: UserRepository by lazy { FirestoreUserRepository() }
}
