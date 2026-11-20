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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DogDetailUiState {
    data object Loading : DogDetailUiState
    data object NotFound : DogDetailUiState
    data object Error : DogDetailUiState

    data class Content(
        val dog: Dog,
        val images: List<DogImage>,
        val traits: List<Trait>,
        val dogTraits: List<DogTrait>,
        val careTips: List<CareTip>,
        val dogCareTips: List<DogCareTip>,
        val isFavorite: Boolean
    ) : DogDetailUiState
}

class DogDetailViewModel(
    private val dogRepository: DogRepository = AppContainer.dogRepository,
    private val favoriteRepository: FavoriteRepository = AppContainer.favoriteRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DogDetailUiState>(DogDetailUiState.Loading)
    val state: StateFlow<DogDetailUiState> = _state.asStateFlow()

    private var loadedDogId: String? = null
    private var favoriteUpdating = false

    fun load(dogId: String) {
        if (loadedDogId == dogId) return
        loadedDogId = dogId
        _state.value = DogDetailUiState.Loading
        viewModelScope.launch {
            try {
                val dog = dogRepository.getDogById(dogId)
                if (dog == null) {
                    _state.value = DogDetailUiState.NotFound
                    return@launch
                }
                val images = dogRepository.getDogImages(dogId)
                val traits = dogRepository.getTraits()
                val dogTraits = dogRepository.getDogTraits(dogId)
                val careTips = dogRepository.getCareTips()
                val dogCareTips = dogRepository.getDogCareTips(dogId)
                val isFavorite = favoriteRepository.isFavorite(dogId)

                _state.value = DogDetailUiState.Content(
                    dog = dog,
                    images = images,
                    traits = traits,
                    dogTraits = dogTraits,
                    careTips = careTips,
                    dogCareTips = dogCareTips,
                    isFavorite = isFavorite
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DogDetailUiState.Error
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value as? DogDetailUiState.Content ?: return
        if (favoriteUpdating) return
        favoriteUpdating = true
        viewModelScope.launch {
            try {
                if (current.isFavorite) {
                    favoriteRepository.removeFavorite(current.dog.id)
                } else {
                    favoriteRepository.addFavorite(current.dog.id)
                }
                _state.value = current.copy(
                    isFavorite = !current.isFavorite
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DogDetailUiState.Error
            } finally {
                favoriteUpdating = false
            }
        }
    }
}