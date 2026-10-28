package com.example.doglens.data.mock

import com.example.doglens.R
import com.example.doglens.data.model.Category
import com.example.doglens.data.model.FeaturedTopic

class ExploreMockData {
    val categories = listOf(
        Category(
            id = "large_dogs",
            name = "Chó cỡ lớn",
            description = "Trên 25kg",
            localImageRes = R.drawable.golden_retriever,
            sortOrder = 1
        ),
        Category(
            id = "medium_dogs",
            name = "Chó cỡ trung",
            description = "10 - 25kg",
            localImageRes = R.drawable.corgi,
            sortOrder = 2
        ),
        Category(
            id = "small_dogs",
            name = "Chó cỡ nhỏ",
            description = "Dưới 10kg",
            localImageRes = R.drawable.dog_small_pomeranian,
            sortOrder = 3
        ),
        Category(
            id = "long_haired_dogs",
            name = "Chó lông dài",
            description = "Lông dày, dài",
            localImageRes = R.drawable.siberian_husky,
            sortOrder = 4
        ),
        Category(
            id = "short_haired_dogs",
            name = "Chó lông ngắn",
            description = "Lông ngắn, mượt",
            localImageRes = R.drawable.french_bulldog,
            sortOrder = 5
        )
    )

    val featuredTopics = listOf(
        FeaturedTopic(
            id = "family_friendly",
            title = "Chó thân thiện\nvới trẻ em",
            localImageRes = R.drawable.topic_kid_friendly_golden,
            sortOrder = 1
        ),
        FeaturedTopic(
            id = "smartest_dogs",
            title = "Chó thông minh\nnhất thế giới",
            localImageRes = R.drawable.topic_smart_border_collie,
            sortOrder = 2
        ),
        FeaturedTopic(
            id = "apartment_dogs",
            title = "Chó phù hợp\nnuôi trong nhà",
            localImageRes = R.drawable.topic_apartment_puppy,
            sortOrder = 3
        )
    )
}