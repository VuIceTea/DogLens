package com.example.doglens.ui.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.SimilarDogItem
import com.example.doglens.data.model.hasComparableSize
import com.example.doglens.data.model.sizeSimilarityTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimilarDogsUiState(
    val isLoading: Boolean = true,
    val sourceDog: Dog? = null,
    val items: List<SimilarDogItem> = emptyList(),
    val traitNames: Map<String, List<String>> = emptyMap(),
    val error: String? = null
)

class SimilarDogsViewModel : ViewModel() {
    private val repository = AppContainer.dogRepository
    private val _state = MutableStateFlow(SimilarDogsUiState())
    val state: StateFlow<SimilarDogsUiState> = _state.asStateFlow()
    private var loadedDogId: String? = null

    fun load(dogId: String, refresh: Boolean = false) {
        if (_state.value.sourceDog != null && !refresh && loadedDogId == dogId) return
        if (refresh) repository.invalidateCatalog()
        loadedDogId = dogId
        viewModelScope.launch {
            _state.value = SimilarDogsUiState(isLoading = true)
            try {
                val dogs = repository.getDogs()
                val source = dogs.firstOrNull { it.id == dogId } ?: error("Không tìm thấy giống chó")
                val items = if (!source.hasComparableSize()) emptyList() else dogs
                    .filter { it.id != source.id }
                    .mapNotNull { candidate ->
                        source.sizeSimilarityTo(candidate)?.let { SimilarDogItem(candidate, it) }
                    }
                    .sortedByDescending { it.similarityPercent }
                    .take(12)
                _state.value = SimilarDogsUiState(
                    isLoading = false,
                    sourceDog = source,
                    items = items
                )
                if (items.isEmpty()) return@launch
                try {
                    val (links, traits) = coroutineScope {
                        val labels = async { repository.getTraits().associateBy { it.id } }
                        repository.getDogTraitsByDog(items.map { it.dog.id }.toSet()) to labels.await()
                    }
                    _state.value = _state.value.copy(traitNames = links.mapValues { (_, dogTraits) ->
                        dogTraits.mapNotNull { traits[it.traitId]?.name }.take(3)
                    })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The size comparison is usable while trait labels are unavailable.
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = SimilarDogsUiState(
                    isLoading = false,
                    error = exception.message ?: "Không thể tải giống tương tự"
                )
            }
        }
    }

}
