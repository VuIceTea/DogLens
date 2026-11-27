package com.example.doglens.data.repository

import com.example.doglens.data.model.DogUseCase
import com.example.doglens.data.model.ExploreBanner
import com.example.doglens.data.model.FeaturedTopic

class ExploreRepository {
    private val catalog = FirestoreCatalog()

    suspend fun getBanner(): ExploreBanner? = catalog.byId("explore", "banner")?.let {
        ExploreBanner(it.string("title"), it.string("description"), it.string("actionText"),
            it.getString("imageUrl"), it.imageResource())
    }

    suspend fun getCategories() = catalog.list("exploreCategories").map { it.toDog() }.sortedBy { it.sortOrder }

    suspend fun getFeaturedTopics() = catalog.list("featuredTopics").map {
        FeaturedTopic(it.id, it.string("title"), it.string("description"), it.getString("imageUrl"),
            it.imageResource(), it.stringList("dogIds"), it.int("sortOrder") ?: 0)
    }.sortedBy { it.sortOrder }

    suspend fun getDogUseCases() = catalog.list("dogUseCases").map {
        DogUseCase(it.id, it.string("name"), it.string("description"), it.getString("imageUrl"),
            it.imageResource(), it.stringList("dogIds"), it.int("sortOrder") ?: 0)
    }.sortedBy { it.sortOrder }
}
