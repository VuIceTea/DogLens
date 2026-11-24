package com.example.doglens.ui.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.SimilarDogItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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

    fun load(dogId: String) {
        if (loadedDogId == dogId) return
        loadedDogId = dogId
        viewModelScope.launch {
            _state.value = SimilarDogsUiState(isLoading = true)
            try {
                val source = repository.getDogById(dogId)
                    ?: error("Không tìm thấy giống chó")
                val dogs = repository.getDogs().filterNot { it.id == source.id }
                val traits = repository.getTraits().associateBy { it.id }
                val traitNames = dogs.associate { dog ->
                    dog.id to repository.getDogTraits(dog.id)
                        .mapNotNull { traits[it.traitId]?.name }
                        .take(3)
                }
                val items = dogs
                    .map { SimilarDogItem(it, calculateSimilarity(source, it)) }
                    .sortedByDescending { it.similarityPercent }

                _state.value = SimilarDogsUiState(
                    isLoading = false,
                    sourceDog = source,
                    items = items,
                    traitNames = traitNames
                )
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

    private fun calculateSimilarity(source: Dog, candidate: Dog): Int {
        var score = 54f
        if (source.sizeCategoryId == candidate.sizeCategoryId) score += 16f
        score += closeness(source.minWeightKg, candidate.minWeightKg, 35, 5f)
        score += closeness(source.maxHeightCm, candidate.maxHeightCm, 50, 5f)
        score += scoreCloseness(source.familyScore, candidate.familyScore)
        score += scoreCloseness(source.kidsScore, candidate.kidsScore)
        score += scoreCloseness(source.trainingScore, candidate.trainingScore)
        score += scoreCloseness(source.apartmentScore, candidate.apartmentScore)
        return score.roundToInt().coerceIn(68, 96)
    }

    private fun closeness(a: Int?, b: Int?, range: Int, points: Float): Float {
        if (a == null || b == null) return 0f
        return (1f - abs(a - b).toFloat() / range).coerceIn(0f, 1f) * points
    }

    private fun scoreCloseness(a: Int?, b: Int?): Float {
        if (a == null || b == null) return 0f
        return (5 - abs(a - b)).coerceAtLeast(0) * 1.25f
    }
}
