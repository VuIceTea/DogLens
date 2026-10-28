
package com.example.doglens.data.repository

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogUseCase
import com.example.doglens.data.model.ExploreBanner
import com.example.doglens.data.model.FeaturedTopic

class ExploreRepository {

    // Banner khám phá
    fun getBanner() = ExploreBanner(
        title = "Hơn 300+ giống chó trên toàn thế giới",
        description = "Tìm hiểu đặc điểm, tính cách, nguồn gốc và hình ảnh của từng giống chó.",
        actionText = "Khám phá ngay",
        localImageRes = R.drawable.img_golden_retriever
    )

    // Danh mục chó
    // Dùng Dog nếu adapter hiện tại nhận List<Dog>

    fun getCategories(): List<Dog> {
        return listOf(
            Dog(
                id = "large_dog",
                name = "Chó cỡ lớn",
                description = "Trên 25kg",
                localImageRes = R.drawable.img_golden_retriever,
                backgroundColorHex = "#E3F0FD",
                sortOrder = 1
            ),
            Dog(
                id = "medium_dog",
                name = "Chó cỡ trung",
                description = "10 - 25kg",
                localImageRes = R.drawable.corgi,
                backgroundColorHex = "#FDF2E5",
                sortOrder = 2
            ),
            Dog(
                id = "small_dog",
                name = "Chó cỡ nhỏ",
                description = "Dưới 10kg",
                localImageRes = R.drawable.dog_small_pomeranian,
                backgroundColorHex = "#FBE9EC",
                sortOrder = 3
            ),
            Dog(
                id = "long_hair_dog",
                name = "Chó lông dài",
                description = "Bộ lông dày, dài",
                localImageRes = R.drawable.siberian_husky,
                backgroundColorHex = "#DFF7EC",
                sortOrder = 4
            ),
            Dog(
                id = "short_hair_dog",
                name = "Chó lông ngắn",
                description = "Bộ lông ngắn, mượt",
                localImageRes = R.drawable.french_bulldog,
                backgroundColorHex = "#ECE6FD",
                sortOrder = 5
            )
        ).sortedBy { it.sortOrder }
    }


    // Chủ đề nổi bật
    fun getFeaturedTopics(): List<FeaturedTopic> {
        return listOf(
            FeaturedTopic(
                id = "kid_friendly",
                title = "Chó thân thiện với trẻ em",
                description = "Những người bạn hiền lành, vui vẻ và phù hợp với gia đình có trẻ nhỏ.",
                localImageRes = R.drawable.topic_kid_friendly_golden,
                dogIds = listOf(
                    "golden_retriever",
                    "labrador_retriever",
                    "beagle",
                    "cavalier_king_charles_spaniel",
                    "collie"
                ),
                sortOrder = 1
            ),
            FeaturedTopic(
                id = "smart_dogs",
                title = "Chó thông minh nhất thế giới",
                description = "Khám phá những giống chó học lệnh nhanh và có khả năng huấn luyện tốt.",
                localImageRes = R.drawable.topic_smart_border_collie,
                dogIds = listOf(
                    "border_collie",
                    "poodle",
                    "german_shepherd",
                    "golden_retriever",
                    "doberman"
                ),
                sortOrder = 2
            ),
            FeaturedTopic(
                id = "apartment_dogs",
                title = "Chó phù hợp nuôi trong nhà",
                description = "Những giống chó thích nghi tốt với không gian sống nhỏ và căn hộ.",
                localImageRes = R.drawable.topic_apartment_puppy,
                dogIds = listOf(
                    "french_bulldog",
                    "pomeranian",
                    "corgi",
                    "shih_tzu",
                    "pug"
                ),
                sortOrder = 3
            )
        ).sortedBy { it.sortOrder }
    }

    // Nhóm công dụng của chó
    fun getDogUseCases(): List<DogUseCase> {
        return listOf(
            DogUseCase(
                id = "working",
                name = "Chó nghiệp vụ",
                description = "Bảo vệ, tìm kiếm",
                localImageRes = R.drawable.group_guard_german_shepherd,
                dogIds = listOf("german_shepherd"),
                sortOrder = 1
            ),
            DogUseCase(
                id = "sled",
                name = "Chó kéo xe",
                description = "Sức bền cao",
                localImageRes = R.drawable.group_sled_husky,
                dogIds = listOf("siberian_husky", "alaskan_malamute"),
                sortOrder = 2
            ),
            DogUseCase(
                id = "hunting",
                name = "Chó săn",
                description = "Khả năng đánh hơi tốt",
                localImageRes = R.drawable.group_hunting_golden,
                dogIds = listOf("beagle", "bloodhound"),
                sortOrder = 3
            ),
            DogUseCase(
                id = "companion",
                name = "Chó cảnh",
                description = "Dễ thương, thân thiện",
                localImageRes = R.drawable.poodle,
                dogIds = listOf("pomeranian", "poodle"),
                sortOrder = 4
            ),
            DogUseCase(
                id = "herding",
                name = "Chó chăn gia súc",
                description = "Thông minh, nhanh nhẹn",
                localImageRes = R.drawable.group_herding_border_collie,
                dogIds = listOf("border_collie", "australian_shepherd"),
                sortOrder = 5
            )
        ).sortedBy { it.sortOrder }
    }
}
