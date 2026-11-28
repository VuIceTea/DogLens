package com.example.doglens.data.repository

import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import com.example.doglens.data.model.ArticleSection

interface ArticleRepository {
    fun invalidateArticleCache() = Unit
    suspend fun getArticles(): List<Article>
    suspend fun getViewedArticles(): List<Article>
    suspend fun getArticleById(id: String): Article?
    suspend fun getArticlesByIds(ids: List<String>): List<Article> {
        val articles = getArticles().associateBy { it.id }
        return ids.mapNotNull(articles::get)
    }
    suspend fun getCategories(): List<ArticleCategory>
    suspend fun getSections(articleId: String): List<ArticleSection>
    suspend fun recordView(articleId: String)
    suspend fun isBookmarked(articleId: String): Boolean
    suspend fun setBookmarked(articleId: String, bookmarked: Boolean)
}
