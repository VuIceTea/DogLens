package com.example.doglens.ui.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.hasComparableSize
import com.example.doglens.data.model.sizeSimilarityTo
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.RecognitionResult
import com.example.doglens.data.model.Trait
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.RecognitionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data object NotFound : ResultUiState
    data class Content(
        val recognition: RecognitionResult,
        val dog: Dog,
        val traits: List<Pair<Trait, DogTrait>>,
        val relatedDogs: List<Dog>,
        val isFavorite: Boolean,
        val favoriteError: Boolean = false
    ) : ResultUiState
}

class ResultViewModel(
    private val recognitionRepository: RecognitionRepository = AppContainer.recognitionRepository,
    private val dogRepository: DogRepository = AppContainer.dogRepository,
    private val favoriteRepository: FavoriteRepository = AppContainer.favoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    private var loadedId: String? = null
    private var favoriteUpdating = false

    fun load(recognitionId: String) {
        if (loadedId == recognitionId) return
        loadedId = recognitionId

        viewModelScope.launch {
            try {
                val recognition = recognitionRepository.getById(recognitionId)
                val dog = recognition?.let { dogRepository.getDogById(it.dogId) }
                if (recognition == null || dog == null) {
                    _state.value = ResultUiState.NotFound
                    return@launch
                }

                val traitsById = dogRepository.getTraits().associateBy { it.id }
                val traits = dogRepository.getDogTraits(dog.id).mapNotNull { dogTrait ->
                    traitsById[dogTrait.traitId]?.let { trait -> trait to dogTrait }
                }
                val relatedDogs = if (!dog.hasComparableSize()) emptyList() else dogRepository.getDogs()
                    .filter { it.id != dog.id }
                    .mapNotNull { candidate -> dog.sizeSimilarityTo(candidate)?.let { candidate to it } }
                    .sortedByDescending { it.second }
                    .take(4)
                    .map { it.first }

                _state.value = ResultUiState.Content(
                    recognition = recognition,
                    dog = dog,
                    traits = traits,
                    relatedDogs = relatedDogs,
                    isFavorite = favoriteRepository.isFavorite(dog.id)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ResultUiState.NotFound
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value as? ResultUiState.Content ?: return
        if (favoriteUpdating) return
        favoriteUpdating = true
        viewModelScope.launch {
            try {
                if (current.isFavorite) favoriteRepository.removeFavorite(current.dog.id)
                else favoriteRepository.addFavorite(current.dog.id)
                _state.value = current.copy(isFavorite = !current.isFavorite, favoriteError = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = current.copy(favoriteError = true)
            } finally {
                favoriteUpdating = false
            }
        }
    }

    fun clearFavoriteError() {
        val current = _state.value as? ResultUiState.Content ?: return
        _state.value = current.copy(favoriteError = false)
    }
}
