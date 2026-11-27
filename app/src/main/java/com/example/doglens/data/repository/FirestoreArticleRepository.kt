package com.example.doglens.data.repository

import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import com.example.doglens.data.model.ArticleSection
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

internal class FirestoreArticleRepository(
    private val catalog: FirestoreCatalog = FirestoreCatalog(),
    private val account: FirestoreAccountData = FirestoreAccountData(catalog.db)
) : ArticleRepository {
    private val appContext get() = FirebaseApp.getInstance().applicationContext
    private val published by lazy {
        appContext.assets.open("published-articles.json").bufferedReader().use { JSONObject(it.readText()) }
    }
    @Volatile private var recentlyReadArticles = emptyMap<String, Article>()

    override suspend fun getArticles() =
        readPublished(catalog.db.collection("articles"), "articles", { it.toArticle() }, ::publishedArticle)
            .sortedByDescending {
            it.publishedAt.split("/").asReversed().joinToString("")
        }.also { articles -> recentlyReadArticles = articles.associateBy { it.id } }

    override suspend fun getViewedArticles(): List<Article> {
        val views = account.collection("articleViews") ?: return emptyList()
        val viewed = views.orderBy("viewedAt", Query.Direction.DESCENDING).get().await().documents
        if (viewed.isEmpty()) return emptyList()
        val byId = getArticles().associateBy { it.id }
        return viewed.mapNotNull { byId[it.id] }
    }

    override suspend fun getArticleById(id: String) =
        recentlyReadArticles[id] ?: getArticles().firstOrNull { it.id == id }

    override suspend fun getCategories() =
        readPublished(catalog.db.collection("articleCategories"), "articleCategories", {
            ArticleCategory(it.id, it.string("name"), it.string("colorHex", "#E3F0FD"), it.int("sortOrder") ?: 0)
        }, {
            ArticleCategory(it.getString("id"), it.getString("name"), it.optString("colorHex", "#E3F0FD"), it.optInt("sortOrder"))
        }).sortedBy { it.sortOrder }

    override suspend fun getSections(articleId: String) =
        readPublished(catalog.db.collection("articleSections").whereEqualTo("articleId", articleId), "articleSections", {
            ArticleSection(it.id, articleId, it.int("sortIndex") ?: 0, it.string("heading"),
                it.string("body"), it.getString("imageUrl"))
        }, { row ->
            ArticleSection(row.getString("id"), articleId, row.optInt("sortIndex"),
                row.getString("heading"), row.getString("body"), row.optString("imageUrl").ifBlank { null })
        }, include = { it.optString("articleId") == articleId }).sortedBy { it.sortIndex }

    private suspend fun <T> readPublished(
        query: Query,
        collection: String,
        fromFirestore: (DocumentSnapshot) -> T,
        fromPackage: (JSONObject) -> T,
        include: (JSONObject) -> Boolean = { true }
    ): List<T> = withContext(Dispatchers.IO) {
        val fresh = withTimeoutOrNull(3_000) {
            try {
                query.get(Source.SERVER).await().documents
            } catch (exception: FirebaseFirestoreException) {
                if (exception.code != FirebaseFirestoreException.Code.UNAVAILABLE &&
                    exception.code != FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) throw exception
                null
            }
        }
        if (fresh != null) return@withContext fresh.map(fromFirestore)
        val cached = query.get(Source.CACHE).await().documents
        if (cached.isNotEmpty()) return@withContext cached.map(fromFirestore)
        val rows = published.getJSONArray(collection)
        (0 until rows.length()).map { rows.getJSONObject(it) }.filter(include).map(fromPackage)
    }

    private fun publishedArticle(row: JSONObject): Article {
        val links = row.optJSONObject("sources") ?: JSONObject()
        return Article(
            row.getString("id"), row.getString("categoryId"), row.getString("title"),
            row.getString("summary"), row.optString("imageUrl").ifBlank { null }, null,
            row.getString("publishedAt"), row.optInt("readMinutes"), row.optInt("viewCount"),
            links.keys().asSequence().associateWith { links.getString(it) }
        )
    }

    override suspend fun recordView(articleId: String) {
        account.collection("articleViews")?.document(articleId)
            ?.set(mapOf("viewedAt" to FieldValue.serverTimestamp()))?.await()
    }

    override suspend fun isBookmarked(articleId: String): Boolean =
        account.collection("articleBookmarks")?.document(articleId)?.get()?.await()?.exists() == true

    override suspend fun setBookmarked(articleId: String, bookmarked: Boolean) {
        val reference = account.requireCollection("articleBookmarks").document(articleId)
        if (bookmarked) reference.set(mapOf("addedAt" to FieldValue.serverTimestamp())).await()
        else reference.delete().await()
    }

    private fun DocumentSnapshot.toArticle() = Article(
        id, string("categoryId"), string("title"), string("summary"), getString("imageUrl"),
        imageResource(), string("publishedAt"), int("readMinutes") ?: 0, int("viewCount") ?: 0, sources()
    )
}
