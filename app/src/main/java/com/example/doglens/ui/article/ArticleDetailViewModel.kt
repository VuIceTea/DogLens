package com.example.doglens.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Article
import com.example.doglens.data.model.ArticleCategory
import com.example.doglens.data.model.ArticleSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val category: ArticleCategory? = null,
    val sections: List<ArticleSection> = emptyList(),
    val isBookmarked: Boolean = false,
    val error: String? = null
)

class ArticleDetailViewModel : ViewModel() {
    private val repository = AppContainer.articleRepository
    private val _state = MutableStateFlow(ArticleDetailUiState())
    val state: StateFlow<ArticleDetailUiState> = _state.asStateFlow()
    private var loadedId: String? = null

    fun load(id: String) {
        if (loadedId == id) return
        loadedId = id
        viewModelScope.launch {
            try {
                val article = repository.getArticleById(id) ?: error("Không tìm thấy bài viết")
                val category = repository.getCategories().firstOrNull { it.id == article.categoryId }
                _state.value = ArticleDetailUiState(
                    isLoading = false,
                    article = article,
                    category = category,
                    sections = repository.getSections(id)
                )
            } catch (exception: Exception) {
                _state.value = ArticleDetailUiState(false, error = exception.message)
            }
        }
    }

    fun toggleBookmark() {
        _state.value = _state.value.copy(isBookmarked = !_state.value.isBookmarked)
    }
}
