package com.example.doglens.data.mock

import com.example.doglens.data.model.User
import com.example.doglens.data.model.profile.ProfileResponse
import com.example.doglens.data.repository.ProfileRepository

class MockProfileRepository : ProfileRepository {
    override suspend fun getProfile(): ProfileResponse {
        return ProfileMockData.profile
    }
}