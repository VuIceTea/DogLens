package com.example.doglens.data.repository

import com.example.doglens.data.model.profile.ProfileResponse

interface ProfileRepository {
    suspend fun getProfile(): ProfileResponse
}