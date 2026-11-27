package com.example.doglens.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PopularDogsState {
    data object Loading : PopularDogsState
    data object Error : PopularDogsState
    data class Content(val dogs: List<Dog>) : PopularDogsState
}

class HomeViewModel : ViewModel() {
    private val _popularDogs = MutableStateFlow<PopularDogsState>(PopularDogsState.Loading)
    val popularDogs = _popularDogs.asStateFlow()

    init {
        loadPopularDogs()
    }

    fun retryPopularDogs() {
        if (_popularDogs.value == PopularDogsState.Error) loadPopularDogs()
    }

    private fun loadPopularDogs() {
        _popularDogs.value = PopularDogsState.Loading
        viewModelScope.launch {
            try {
                val dogs = withContext(Dispatchers.IO) {
                    // First access initializes Firestore and its auth provider.
                    AppContainer.dogRepository.getPopularDogs()
                        .map { it.copy(backgroundColorHex = "#E3E2E2") }
                }
                _popularDogs.value = PopularDogsState.Content(dogs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _popularDogs.value = PopularDogsState.Error
            }
        }
    }
}
