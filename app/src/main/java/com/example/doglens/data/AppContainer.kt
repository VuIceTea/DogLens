package com.example.doglens.data

import com.example.doglens.data.repository.ArticleRepository
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.FirestoreUserRepository
import com.example.doglens.data.repository.FirestoreDogRepository
import com.example.doglens.data.repository.FirestoreFavoriteRepository
import com.example.doglens.data.repository.FirestoreArticleRepository
import com.example.doglens.data.repository.FirestoreRecognitionRepository
import com.example.doglens.data.repository.RecognitionRepository
import com.example.doglens.data.repository.UserRepository

object AppContainer {
    val dogRepository: DogRepository by lazy { FirestoreDogRepository() }
    val favoriteRepository: FavoriteRepository by lazy { FirestoreFavoriteRepository(dogRepository) }
    val recognitionRepository: RecognitionRepository by lazy { FirestoreRecognitionRepository(dogRepository) }
    val articleRepository: ArticleRepository by lazy { FirestoreArticleRepository() }
    val userRepository: UserRepository by lazy { FirestoreUserRepository() }
}
