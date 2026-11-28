package com.example.doglens.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogUseCase
import com.example.doglens.data.model.ExploreBanner
import com.example.doglens.data.model.FeaturedTopic
import com.example.doglens.data.repository.ExploreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ExploreContent(
    val categories: List<Dog>,
    val topics: List<FeaturedTopic>,
    val useCases: List<DogUseCase>,
    val banner: ExploreBanner?
)

internal class ExploreViewModel : ViewModel() {
    private val repository = ExploreRepository()
    private val _content = MutableStateFlow<ExploreContent?>(null)
    val content = _content.asStateFlow()
    private var loadJob: Job? = null
    var loadFailed = false
        private set

    fun load(): Job {
        loadJob?.takeIf { it.isActive }?.let { return it }
        return viewModelScope.launch {
            loadFailed = false
            try {
                val content = withTimeoutOrNull(8_000) {
                    coroutineScope {
                        val categories = async { repository.getCategories() }
                        val topics = async { repository.getFeaturedTopics() }
                        val useCases = async { repository.getDogUseCases() }
                        val banner = async { repository.getBanner() }
                        ExploreContent(categories.await(), topics.await(), useCases.await(), banner.await())
                    }
                }
                if (content == null) loadFailed = true else _content.value = content
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
            }
        }.also { loadJob = it }
    }
}
