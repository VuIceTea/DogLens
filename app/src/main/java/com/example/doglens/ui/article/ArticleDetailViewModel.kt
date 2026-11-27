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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val category: ArticleCategory? = null,
    val sections: List<ArticleSection> = emptyList(),
    val isBookmarked: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class ArticleDetailViewModel : ViewModel() {
    private val repository = AppContainer.articleRepository
    private val _state = MutableStateFlow(ArticleDetailUiState())
    val state: StateFlow<ArticleDetailUiState> = _state.asStateFlow()
    private var loadedId: String? = null

    fun load(id: String, retry: Boolean = false) {
        if (!retry && loadedId == id) return
        if (retry) _state.value = ArticleDetailUiState()
        loadedId = id
        viewModelScope.launch {
            try {
                val article = repository.getArticleById(id) ?: error("Không tìm thấy bài viết")
                _state.value = coroutineScope {
                    val category = async { repository.getCategories().firstOrNull { it.id == article.categoryId } }
                    val sections = async { repository.getSections(id) }
                    ArticleDetailUiState(
                        isLoading = false,
                        article = article,
                        category = category.await(),
                        sections = sections.await()
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = ArticleDetailUiState(isLoading = false, error = "Không thể tải bài viết.")
                return@launch
            }
            launch {
                ignoreAccountFailure { repository.recordView(id) }
            }
            val bookmark = withTimeoutOrNull(3_000) {
                ignoreAccountFailure { repository.isBookmarked(id) }
            }
            if (bookmark != null && !_state.value.isSaving && !bookmarkChanged) {
                _state.value = _state.value.copy(isBookmarked = bookmark)
            }
        }
    }

    private var bookmarkChanged = false

    private suspend fun <T> ignoreAccountFailure(operation: suspend () -> T): T? =
        try {
            operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    fun toggleBookmark() {
        val current = _state.value
        val articleId = current.article?.id ?: return
        if (current.isSaving) return
        bookmarkChanged = true
        _state.value = current.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                repository.setBookmarked(articleId, !current.isBookmarked)
                _state.value = _state.value.copy(isBookmarked = !current.isBookmarked)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = _state.value.copy(error = exception.message ?: "Không lưu được bài viết.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
