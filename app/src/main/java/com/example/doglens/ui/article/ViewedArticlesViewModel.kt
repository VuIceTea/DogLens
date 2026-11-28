package com.example.doglens.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ViewedArticleRow(val article: Article, val category: ArticleCategory)

data class ViewedArticlesUiState(
    val isLoading: Boolean = true,
    val rows: List<ViewedArticleRow> = emptyList(),
    val selectedCategory: String? = null,
    val query: String = "",
    val error: String? = null
)

class ViewedArticlesViewModel : ViewModel() {
    private val repository = AppContainer.articleRepository
    private val _state = MutableStateFlow(ViewedArticlesUiState())
    val state: StateFlow<ViewedArticlesUiState> = _state.asStateFlow()
    private var allRows: List<ViewedArticleRow> = emptyList()
    private var loadJob: Job? = null

    fun load(browse: Boolean, breedId: String?): Job {
        loadJob?.takeIf { it.isActive }?.let { return it }
        return viewModelScope.launch {
            try {
                val categoryRequest = async { repository.getCategories().associateBy { it.id } }
                val articles = if (browse) repository.getArticles() else repository.getViewedArticles()
                val categories = categoryRequest.await()
                allRows = articles.filter { breedId == null || it.breedId == breedId }.mapNotNull { article ->
                    categories[article.categoryId]?.let { ViewedArticleRow(article, it) }
                }
                publish(finished = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = exception.message)
            }
        }.also { loadJob = it }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value.trim().lowercase(Locale.getDefault()))
        publish()
    }

    fun setCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategory = categoryId)
        publish()
    }

    private fun publish(finished: Boolean = false) {
        if (!finished && _state.value.isLoading) return
        val current = _state.value
        val rows = allRows.filter { row ->
            (current.selectedCategory == null || row.article.categoryId == current.selectedCategory) &&
                (current.query.isBlank() || row.article.title.contains(current.query, ignoreCase = true) ||
                    row.article.summary.contains(current.query, ignoreCase = true))
        }
        _state.value = current.copy(isLoading = false, rows = rows, error = null)
    }
}
