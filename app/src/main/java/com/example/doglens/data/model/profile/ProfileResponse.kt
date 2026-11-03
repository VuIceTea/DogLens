package com.example.doglens.data.model.profile

import com.example.doglens.data.model.User

data class ProfileResponse(
    val user: User,
    val stats: ProfileStats
)