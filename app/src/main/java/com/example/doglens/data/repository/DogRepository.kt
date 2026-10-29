package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog

class DogRepository {
    fun getDogs(): List<Dog> = listOf(
        Dog(
            id = "golden_retriever",
            name = "Golden Retriever",
            description = "Thân thiện, thông minh, dễ huấn luyện",
            localImageRes = R.drawable.img_golden_retriever,
            sizeCategoryId = "large",
            minWeightKg = 25,
            maxWeightKg = 34,
            minHeightCm = 51,
            maxHeightCm = 61
        ),
        Dog(
            id = "corgi",
            name = "Corgi",
            description = "Vui vẻ, năng động, thân thiện",
            localImageRes = R.drawable.corgi,
            sizeCategoryId = "medium",
            minWeightKg = 10,
            maxWeightKg = 14,
            minHeightCm = 25,
            maxHeightCm = 30
        ),
        Dog(
            id = "siberian_husky",
            name = "Husky",
            description = "Năng động, thông minh, độc lập",
            localImageRes = R.drawable.siberian_husky,
            sizeCategoryId = "large",
            minWeightKg = 16,
            maxWeightKg = 27,
            minHeightCm = 51,
            maxHeightCm = 60
        ),
        Dog(
            id = "poodle",
            name = "Poodle",
            description = "Thông minh, dễ huấn luyện, thân thiện",
            localImageRes = R.drawable.poodle,
            sizeCategoryId = "small",
            minWeightKg = 6,
            maxWeightKg = 10,
            minHeightCm = 24,
            maxHeightCm = 28
        ),
        Dog(
            id = "french_bulldog",
            name = "French Bulldog",
            description = "Hiền lành, tình cảm, phù hợp ở căn hộ",
            localImageRes = R.drawable.french_bulldog,
            sizeCategoryId = "small",
            minWeightKg = 8,
            maxWeightKg = 14,
            minHeightCm = 28,
            maxHeightCm = 33
        ),
        Dog(
            id = "pomeranian",
            name = "Pomeranian",
            description = "Hoạt bát, thông minh, đáng yêu",
            localImageRes = R.drawable.dog_small_pomeranian,
            sizeCategoryId = "small",
            minWeightKg = 2,
            maxWeightKg = 4,
            minHeightCm = 18,
            maxHeightCm = 24
        )
    )
}