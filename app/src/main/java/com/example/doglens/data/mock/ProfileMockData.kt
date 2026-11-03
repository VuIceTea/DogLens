package com.example.doglens.data.mock

import com.example.doglens.data.model.User
import com.example.doglens.data.model.profile.ProfileResponse
import com.example.doglens.data.model.profile.ProfileStats

object ProfileMockData {

    val profile = ProfileResponse(
        user = User(
            id = "user_001",
            fullName = "Nguyễn Phi Vũ",
            email = "nguyenphivu@gmail.com",
            avatarUrl = null,
            membership = "FREE"
        ),
        stats = ProfileStats(
            saveDogs = 12,
            recognitionCount = 28,
            viewedArticles = 5
        )
    )
}