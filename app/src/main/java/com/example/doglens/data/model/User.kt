package com.example.doglens.data.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val membership: String = "FREE",
    val authProvider: String = "email",
    val createdAt: String = "",

    // Thông tin cá nhân
    val phoneNumber: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val address: String = "",
    val customAvatarUrl: String? = null,
    val hasCustomAvatar: Boolean = false,
    val premiumUntil: Long? = null
)
