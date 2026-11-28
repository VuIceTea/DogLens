package com.example.doglens.ui.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.CareTip
import com.example.doglens.data.model.DogCareTip
import com.example.doglens.data.model.hasComparableSize
import com.example.doglens.data.model.sizeSimilarityTo
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.RecognitionResult
import com.example.doglens.data.model.Trait
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.RecognitionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
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
        val care: List<Pair<CareTip, DogCareTip>>,
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
    private var favoriteVersion = 0
    private var favoriteObserver: Job? = null
    private var loadJob: Job? = null

    fun load(recognitionId: String, refresh: Boolean = false) {
        if (loadJob?.isActive == true || (!refresh && loadedId == recognitionId)) return
        loadedId = recognitionId
        _state.value = ResultUiState.Loading

        loadJob = viewModelScope.launch {
            try {
                val recognition = recognitionRepository.getById(recognitionId)
                val dog = recognition?.let { dogRepository.getDogById(it.dogId) }
                if (recognition == null || dog == null) {
                    _state.value = ResultUiState.NotFound
                    return@launch
                }

                val traits = async {
                    optionalDetails {
                        val links = dogRepository.getDogTraits(dog.id)
                        val labels = if (links.isEmpty()) emptyMap() else dogRepository.getTraits().associateBy { it.id }
                        links.mapNotNull { link -> labels[link.traitId]?.let { it to link } }
                    }
                }
                val care = async {
                    optionalDetails {
                        val links = dogRepository.getDogCareTips(dog.id)
                        val labels = if (links.isEmpty()) emptyMap() else dogRepository.getCareTips().associateBy { it.id }
                        links.mapNotNull { link -> labels[link.careTipId]?.let { it to link } }
                    }
                }
                val relatedDogs = async {
                    optionalDetails {
                        if (!dog.hasComparableSize()) emptyList() else dogRepository.getDogs()
                            .filter { it.id != dog.id }
                            .mapNotNull { candidate -> dog.sizeSimilarityTo(candidate)?.let { candidate to it } }
                            .sortedByDescending { it.second }
                            .take(4)
                            .map { it.first }
                    }
                }

                _state.value = ResultUiState.Content(
                    recognition = recognition,
                    dog = dog,
                    traits = traits.await(),
                    care = care.await(),
                    relatedDogs = relatedDogs.await(),
                    isFavorite = false
                )
                favoriteObserver?.cancel()
                favoriteObserver = viewModelScope.launch {
                    favoriteRepository.observeFavorite(dog.id)
                        .catch { if (it is CancellationException) throw it }
                        .collect { favorite ->
                            val current = _state.value as? ResultUiState.Content ?: return@collect
                            if (current.dog.id == dog.id && (!favoriteUpdating || current.isFavorite == favorite.saved)) {
                                favoriteUpdating = false
                                _state.value = current.copy(isFavorite = favorite.saved)
                            }
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ResultUiState.NotFound
            }
        }
    }

    private suspend fun <T> optionalDetails(load: suspend () -> List<T>): List<T> =
        try {
            kotlinx.coroutines.withTimeoutOrNull(4_000) { load() }.orEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }

    fun toggleFavorite() {
        val current = _state.value as? ResultUiState.Content ?: return
        if (favoriteUpdating) return
        favoriteUpdating = true
        val version = ++favoriteVersion
        _state.value = current.copy(isFavorite = !current.isFavorite, favoriteError = false)
        viewModelScope.launch {
            try {
                if (current.isFavorite) favoriteRepository.removeFavorite(current.dog.id)
                else favoriteRepository.addFavorite(current.dog.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val latest = _state.value as? ResultUiState.Content
                if (version == favoriteVersion && latest?.recognition?.id == current.recognition.id) {
                    _state.value = latest.copy(isFavorite = current.isFavorite, favoriteError = true)
                }
            } finally {
                if (version == favoriteVersion) favoriteUpdating = false
            }
        }
    }

    fun clearFavoriteError() {
        val current = _state.value as? ResultUiState.Content ?: return
        _state.value = current.copy(favoriteError = false)
    }
}
