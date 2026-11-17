package com.example.doglens.data.mock

import com.example.doglens.R
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogSizeCategory
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait
import com.example.doglens.data.repository.DogRepository

class MockDogRepository : DogRepository {

    private val sizeCategories = listOf(
        DogSizeCategory("small", "Chó nhỏ", 1),
        DogSizeCategory("medium", "Chó trung", 2),
        DogSizeCategory("large", "Chó lớn", 3)
    )

    private val dogs = listOf(
        Dog(
            id = "golden_retriever",
            name = "Golden Retriever",
            description = "Thân thiện, thông minh, dễ huấn luyện",
            localImageRes = R.drawable.img_golden_retriever,
            sizeCategoryId = "large",
            minWeightKg = 25,
            maxWeightKg = 34,
            minHeightCm = 51,
            maxHeightCm = 61,
            origin = "Scotland",
            minLifespanYears = 10,
            maxLifespanYears = 12,
            coatColors = "Vàng, vàng kem, vàng đậm",
            familyScore = 5,
            kidsScore = 5,
            trainingScore = 4,
            apartmentScore = 3,
            summary = "Golden Retriever là giống chó thân thiện, thông minh và dễ huấn luyện. " +
                    "Đây là một trong những giống chó phổ biến nhất trên thế giới."
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
            maxHeightCm = 30,
            origin = "Xứ Wales (Anh)",
            minLifespanYears = 12,
            maxLifespanYears = 13,
            coatColors = "Đỏ, vàng nâu, tam thể",
            familyScore = 5,
            kidsScore = 4,
            trainingScore = 4,
            apartmentScore = 4,
            summary = "Corgi có chân ngắn, thân dài và tính cách vui vẻ. " +
                    "Vốn là chó chăn gia súc nên rất lanh lợi và thích vận động."
        ),
        Dog(
            id = "siberian_husky", name = "Husky",
            description = "Năng động, thông minh, độc lập",
            localImageRes = R.drawable.siberian_husky,
            sizeCategoryId = "large",
            minWeightKg = 16,
            maxWeightKg = 27,
            minHeightCm = 51,
            maxHeightCm = 60,
            origin = "Siberia (Nga)",
            minLifespanYears = 12,
            maxLifespanYears = 14,
            coatColors = "Đen trắng, xám trắng, đỏ trắng",
            familyScore = 4,
            kidsScore = 4,
            trainingScore = 3,
            apartmentScore = 2,
            summary = "Siberian Husky là giống chó kéo xe có sức bền rất cao, " +
                    "nổi bật với đôi mắt xanh và bộ lông dày hai lớp."
        ),
        Dog(
            id = "poodle", name = "Poodle",
            description = "Thông minh, dễ huấn luyện, thân thiện",
            localImageRes = R.drawable.poodle,
            sizeCategoryId = "small",
            minWeightKg = 6,
            maxWeightKg = 10,
            minHeightCm = 24,
            maxHeightCm = 28,
            origin = "Pháp",
            minLifespanYears = 12,
            maxLifespanYears = 15,
            coatColors = "Nâu đỏ, mơ, trắng, đen",
            familyScore = 5,
            kidsScore = 4,
            trainingScore = 5,
            apartmentScore = 5,
            summary = "Poodle là một trong những giống chó thông minh nhất, " +
                    "lông xoăn ít rụng nên rất hợp với gia đình ở căn hộ."
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
            maxHeightCm = 33,
            origin = "Pháp",
            minLifespanYears = 10,
            maxLifespanYears = 12,
            coatColors = "Vện, kem, nâu vàng, trắng",
            familyScore = 5,
            kidsScore = 4,
            trainingScore = 3,
            apartmentScore = 5,
            summary = "French Bulldog có đôi tai dơi đặc trưng, tính cách điềm tĩnh, " +
                    "ít sủa và rất hợp với cuộc sống trong căn hộ."
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
            maxHeightCm = 24,
            origin = "Pomerania (Đức – Ba Lan)",
            minLifespanYears = 12,
            maxLifespanYears = 16,
            coatColors = "Cam, kem, trắng, đen",
            familyScore = 4,
            kidsScore = 3,
            trainingScore = 4,
            apartmentScore = 5,
            summary = "Pomeranian nhỏ nhắn với bộ lông xù như cục bông, " +
                    "hoạt bát, tự tin và rất quấn chủ."
        )
    )

    private val traits = listOf(
        Trait("friendly", "Thân thiện", "smile", "#FFF4DC"),
        Trait("smart", "Thông minh", "brain", "#F1E9FF"),
        Trait("loyal", "Trung thành", "heart", "#FFE9EE"),
        Trait("active", "Năng động", "run", "#FFF1DE"),
        Trait("gentle", "Hiền lành", "leaf", "#E4F8EC"),
        Trait("independent", "Độc lập", "star", "#E6F1FD")
    )

    private val dogTraits = listOf(
        DogTrait("golden_retriever", "friendly", "Hòa đồng, gần gũi"),
        DogTrait("golden_retriever", "smart", "Dễ huấn luyện"),
        DogTrait("golden_retriever", "loyal", "Rất yêu chủ"),
        DogTrait("golden_retriever", "active", "Thích vận động"),

        DogTrait("corgi", "active", "Hoạt bát, lanh lợi"),
        DogTrait("corgi", "smart", "Học lệnh nhanh"),
        DogTrait("corgi", "friendly", "Vui vẻ với mọi người"),
        DogTrait("corgi", "loyal", "Gắn bó gia đình"),

        DogTrait("siberian_husky", "active", "Sức bền rất cao"),
        DogTrait("siberian_husky", "independent", "Có chính kiến"),
        DogTrait("siberian_husky", "friendly", "Thân thiện, ít cắn"),
        DogTrait("siberian_husky", "smart", "Thông minh, tò mò"),

        DogTrait("poodle", "smart", "Top giống thông minh"),
        DogTrait("poodle", "friendly", "Thân thiện, tình cảm"),
        DogTrait("poodle", "active", "Hoạt bát, ham chơi"),
        DogTrait("poodle", "gentle", "Ít rụng lông"),

        DogTrait("french_bulldog", "gentle", "Điềm tĩnh, ít sủa"),
        DogTrait("french_bulldog", "friendly", "Tình cảm, quấn chủ"),
        DogTrait("french_bulldog", "loyal", "Trung thành"),
        DogTrait("french_bulldog", "independent", "Hợp ở căn hộ"),

        DogTrait("pomeranian", "active", "Hoạt bát, lanh lợi"),
        DogTrait("pomeranian", "smart", "Thông minh"),
        DogTrait("pomeranian", "loyal", "Rất quấn chủ"),
        DogTrait("pomeranian", "independent", "Tự tin, cá tính")
    )

    override suspend fun getDogs(): List<Dog> = dogs

    override suspend fun getDogById(id: String): Dog? = dogs.firstOrNull { it.id == id }

    override suspend fun getTraits(): List<Trait> = traits

    override suspend fun getDogTraits(dogId: String): List<DogTrait> =
        dogTraits.filter { it.dogId == dogId }

    override suspend fun getSizeCategories(): List<DogSizeCategory> = sizeCategories
}