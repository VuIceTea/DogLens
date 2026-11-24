package com.example.doglens.ui.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class SuggestedFilter { ALL, POPULAR, SIZE, TRAIT, ORIGIN }

data class SuggestedDogRow(
    val dog: Dog,
    val traits: List<String>
)

data class SuggestedDogsUiState(
    val isLoading: Boolean = true,
    val rows: List<SuggestedDogRow> = emptyList(),
    val selectedFilter: SuggestedFilter = SuggestedFilter.ALL,
    val query: String = "",
    val error: String? = null
)

class SuggestedDogsViewModel : ViewModel() {
    private val repository = AppContainer.dogRepository
    private val _state = MutableStateFlow(SuggestedDogsUiState())
    val state: StateFlow<SuggestedDogsUiState> = _state.asStateFlow()

    private var sourceDog: Dog? = null
    private var allRows: List<SuggestedDogRow> = emptyList()
    private var traitIdsByDog: Map<String, Set<String>> = emptyMap()
    private var loadedSourceId: String? = null
    private var hasLoaded = false

    fun load(sourceDogId: String?) {
        if (hasLoaded && loadedSourceId == sourceDogId) return
        hasLoaded = true
        loadedSourceId = sourceDogId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                sourceDog = sourceDogId?.let { repository.getDogById(it) }
                val dogs = repository.getDogs()
                val traits = repository.getTraits().associateBy { it.id }
                traitIdsByDog = dogs.associate { dog ->
                    dog.id to repository.getDogTraits(dog.id).map { it.traitId }.toSet()
                }
                allRows = dogs.filterNot { it.id == sourceDog?.id }.map { dog ->
                    SuggestedDogRow(
                        dog = dog,
                        traits = traitIdsByDog[dog.id].orEmpty()
                            .mapNotNull { traits[it]?.name }
                            .take(3)
                    )
                }
                publish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Không thể tải gợi ý"
                )
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(
            query = value.trim().lowercase(Locale.getDefault())
        )
        publish()
    }

    fun setFilter(filter: SuggestedFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
        publish()
    }

    private fun publish() {
        if (allRows.isEmpty() && _state.value.isLoading) return
        val current = _state.value
        val source = sourceDog
        val sourceTraits = source?.let { traitIdsByDog[it.id].orEmpty() }.orEmpty()
        var rows = allRows.filter { row ->
            val dog = row.dog
            val matchesQuery = current.query.isBlank() || listOf(
                dog.name,
                dog.description,
                dog.summary.orEmpty(),
                dog.origin.orEmpty(),
                row.traits.joinToString(" ")
            ).any { it.lowercase(Locale.getDefault()).contains(current.query) }
            val matchesFilter = when (current.selectedFilter) {
                SuggestedFilter.ALL, SuggestedFilter.POPULAR -> true
                SuggestedFilter.SIZE -> source == null || dog.sizeCategoryId == source.sizeCategoryId
                SuggestedFilter.TRAIT -> source == null || traitIdsByDog[dog.id].orEmpty().any { it in sourceTraits }
                SuggestedFilter.ORIGIN -> source == null || normalizeOrigin(dog.origin) == normalizeOrigin(source.origin)
            }
            matchesQuery && matchesFilter
        }
        rows = if (current.selectedFilter == SuggestedFilter.POPULAR) {
            rows.sortedByDescending { it.dog.favoriteCount ?: 0 }
        } else {
            rows.sortedBy { it.dog.sortOrder }
        }
        _state.value = current.copy(isLoading = false, rows = rows, error = null)
    }

    private fun normalizeOrigin(value: String?): String =
        value.orEmpty().substringBefore("(").trim().lowercase(Locale.getDefault())
}
