package com.example.doglens.data.repository

import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import com.example.doglens.data.model.ArticleSection

interface ArticleRepository {
    suspend fun getViewedArticles(): List<Article>
    suspend fun getArticleById(id: String): Article?
    suspend fun getCategories(): List<ArticleCategory>
    suspend fun getSections(articleId: String): List<ArticleSection>
}
