package com.example.doglens.data.model

data class Dog (
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val backgroundColorHex: String = "#E3F0FD",
    val sortOrder: Int = 0,

    val sizeCategoryId: String = "medium",
    val minWeightKg: Int? = null,
    val maxWeightKg: Int? = null,
    val minHeightCm: Int? = null,
    val maxHeightCm: Int? = null,

    val origin: String? = null,
    val minLifespanYears: Int? = null,
    val maxLifespanYears: Int? = null,
    val coatColors: String? = null,
    // mức phù hợp 1..5
    val familyScore: Int? = null,
    val kidsScore: Int? = null,
    val trainingScore: Int? = null,
    val apartmentScore: Int? = null,

    // đoạn giới thiệu dài
    val summary: String? = null,
    val rating: Double? = null,
    val favoriteCount: Int? = null,
    val exploreDogIds: List<String> = emptyList(),
    val lifespanMedianYears: Double? = null,
    val heightStandard: String? = null,
    val weightStandard: String? = null,
    val weightMedianKg: Double? = null,
    val weightReferenceKg: Double? = null,
    val lifespanReference: String? = null,
    val sources: Map<String, String> = emptyMap(),
    val searchAliases: List<String> = emptyList(),
    val longDescription: String? = null
)
