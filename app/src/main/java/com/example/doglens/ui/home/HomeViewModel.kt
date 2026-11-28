package com.example.doglens.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface PopularDogsState {
    data object Loading : PopularDogsState
    data object Error : PopularDogsState
    data class Content(val dogs: List<Dog>) : PopularDogsState
}

class HomeViewModel : ViewModel() {
    private val _popularDogs = MutableStateFlow<PopularDogsState>(PopularDogsState.Loading)
    val popularDogs = _popularDogs.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadPopularDogs()
    }

    fun retryPopularDogs() {
        if (_popularDogs.value == PopularDogsState.Error) loadPopularDogs()
    }

    fun refreshPopularDogs(): Job = loadPopularDogs(refresh = true)

    private fun loadPopularDogs(refresh: Boolean = false): Job {
        loadJob?.takeIf { it.isActive }?.let { return it }
        if (!refresh || _popularDogs.value !is PopularDogsState.Content) _popularDogs.value = PopularDogsState.Loading
        return viewModelScope.launch {
            try {
                val dogs = withTimeoutOrNull(8_000) { withContext(Dispatchers.IO) {
                    // First access initializes Firestore and its auth provider.
                    AppContainer.dogRepository.getPopularDogs()
                        .map { it.copy(backgroundColorHex = "#E3E2E2") }
                } } ?: error("Không thể tải giống chó phổ biến")
                _popularDogs.value = PopularDogsState.Content(dogs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _popularDogs.value = PopularDogsState.Error
            }
        }.also { loadJob = it }
    }
}
