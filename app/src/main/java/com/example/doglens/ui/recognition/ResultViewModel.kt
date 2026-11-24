package com.example.doglens.ui.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
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
        val isFavorite: Boolean
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
                val relatedDogs = dogRepository.getDogs()
                    .filterNot { it.id == dog.id }
                    .sortedByDescending { candidate ->
                        var score = 0
                        if (candidate.sizeCategoryId == dog.sizeCategoryId) score += 20
                        score += 5 - kotlin.math.abs(
                            (candidate.familyScore ?: 0) - (dog.familyScore ?: 0)
                        )
                        score += 5 - kotlin.math.abs(
                            (candidate.trainingScore ?: 0) - (dog.trainingScore ?: 0)
                        )
                        score
                    }
                    .take(4)

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
        viewModelScope.launch {
            if (current.isFavorite) favoriteRepository.removeFavorite(current.dog.id)
            else favoriteRepository.addFavorite(current.dog.id)
            _state.value = current.copy(isFavorite = !current.isFavorite)
        }
    }
}
