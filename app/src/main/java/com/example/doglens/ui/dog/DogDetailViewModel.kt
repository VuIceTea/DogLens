package com.example.doglens.ui.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.CareTip
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogCareTip
import com.example.doglens.data.model.DogImage
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DogDetailUiState {
    data object Loading : DogDetailUiState
    data object NotFound : DogDetailUiState
    data object Error : DogDetailUiState

    data class Content(
        val dog: Dog,
        val images: List<DogImage> = emptyList(),
        val traits: List<Trait> = emptyList(),
        val dogTraits: List<DogTrait> = emptyList(),
        val careTips: List<CareTip> = emptyList(),
        val dogCareTips: List<DogCareTip> = emptyList(),
        val isFavorite: Boolean = false,
        val favoritePending: Boolean = false,
        val detailsLoading: Boolean = true,
        val detailsError: Boolean = false,
        val favoriteError: Boolean = false
    ) : DogDetailUiState
}

class DogDetailViewModel(
    private val dogRepository: DogRepository = AppContainer.dogRepository,
    private val favoriteRepository: FavoriteRepository = AppContainer.favoriteRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DogDetailUiState>(DogDetailUiState.Loading)
    val state: StateFlow<DogDetailUiState> = _state.asStateFlow()

    private var loadedDogId: String? = null
    private var loadJob: Job? = null
    private var favoriteObserver: Job? = null
    private var favoriteVersion = 0
    private var requestedFavorite: Boolean? = null

    fun load(dogId: String, retry: Boolean = false) {
        if (!retry && loadedDogId == dogId) return
        loadJob?.cancel()
        loadedDogId = dogId
        val cached = dogRepository.cachedDog(dogId)
        val current = (_state.value as? DogDetailUiState.Content)?.takeIf { it.dog.id == dogId }
        _state.value = current?.copy(detailsLoading = true, detailsError = false)
            ?: cached?.let { DogDetailUiState.Content(it) } ?: DogDetailUiState.Loading
        favoriteObserver?.cancel()
        if (current == null) requestedFavorite = null
        if (_state.value is DogDetailUiState.Content) observeFavorite(dogId)
        loadJob = viewModelScope.launch {
            try {
                val lookup = read { dogRepository.getDogById(dogId) }
                val dog = lookup.getOrElse {
                    cached ?: run {
                        _state.value = DogDetailUiState.Error
                        return@launch
                    }
                }
                if (dog == null) {
                    _state.value = DogDetailUiState.NotFound
                    return@launch
                }
                val displayed = _state.value as? DogDetailUiState.Content
                _state.value = displayed?.copy(dog = dog) ?: DogDetailUiState.Content(dog)
                if (displayed == null) observeFavorite(dogId)
                loadDetails(dogId, lookup.isFailure)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DogDetailUiState.Error
            }
        }
    }

    private suspend fun loadDetails(dogId: String, refreshFailed: Boolean) = coroutineScope {
        val images = async { read { dogRepository.getDogImages(dogId) } }
        val traits = async {
            read {
                val links = dogRepository.getDogTraits(dogId)
                val labels = if (links.isEmpty()) emptyList() else dogRepository.getTraits()
                labels to links
            }
        }
        val care = async {
            read {
                val links = dogRepository.getDogCareTips(dogId)
                val tips = if (links.isEmpty()) emptyList() else dogRepository.getCareTips()
                tips to links
            }
        }
        val gallery = images.await()
        val traitData = traits.await()
        val careData = care.await()
        updateContent {
            it.copy(
                images = gallery.getOrDefault(emptyList()),
                traits = traitData.getOrNull()?.first.orEmpty(),
                dogTraits = traitData.getOrNull()?.second.orEmpty(),
                careTips = careData.getOrNull()?.first.orEmpty(),
                dogCareTips = careData.getOrNull()?.second.orEmpty(),
                detailsLoading = false,
                detailsError = it.detailsError || refreshFailed || gallery.isFailure || traitData.isFailure || careData.isFailure
            )
        }
    }

    private fun observeFavorite(dogId: String) {
        favoriteObserver = viewModelScope.launch {
            favoriteRepository.observeFavorite(dogId)
                .catch { if (it is CancellationException) throw it }
                .collect { status ->
                    val target = requestedFavorite
                    if (target != null && target != status.saved) return@collect
                    requestedFavorite = null
                    updateContent { it.copy(isFavorite = status.saved, favoritePending = status.pending) }
                }
        }
    }

    private suspend fun <T> read(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun updateContent(transform: (DogDetailUiState.Content) -> DogDetailUiState.Content) {
        val current = _state.value as? DogDetailUiState.Content ?: return
        _state.value = transform(current)
    }

    fun toggleFavorite() {
        val current = _state.value as? DogDetailUiState.Content ?: return
        if (requestedFavorite != null) return
        val target = !current.isFavorite
        val version = ++favoriteVersion
        requestedFavorite = target
        updateContent { it.copy(isFavorite = target, favoritePending = true, favoriteError = false) }
        viewModelScope.launch {
            try {
                if (current.isFavorite) {
                    favoriteRepository.removeFavorite(current.dog.id)
                } else {
                    favoriteRepository.addFavorite(current.dog.id)
                }
                if (version == favoriteVersion && loadedDogId == current.dog.id) {
                    requestedFavorite = null
                    updateContent { it.copy(isFavorite = target, favoritePending = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (version == favoriteVersion && loadedDogId == current.dog.id) {
                    requestedFavorite = null
                    updateContent { it.copy(isFavorite = current.isFavorite, favoritePending = false, favoriteError = true) }
                }
            }
        }
    }

    fun clearFavoriteError() {
        val current = _state.value as? DogDetailUiState.Content ?: return
        _state.value = current.copy(favoriteError = false)
    }
}
