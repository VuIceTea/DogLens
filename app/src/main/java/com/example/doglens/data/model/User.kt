package com.example.doglens.data.model

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String?,
    val membership: String
)