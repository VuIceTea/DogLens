package com.example.doglens.ui.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.Dog
import com.example.doglens.data.model.DogTrait
import com.example.doglens.data.model.Trait
import com.example.doglens.data.repository.ExploreRepository
import com.example.doglens.ui.explore.ExploreSection
import com.example.doglens.ui.explore.getGroups
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private var loadJob: Job? = null

    internal fun load(sourceDogId: String?, section: ExploreSection?, groupId: String?, refresh: Boolean = false) {
        if (loadJob?.isActive == true || (!refresh && hasLoaded && loadedSourceId == sourceDogId)) return
        loadedSourceId = sourceDogId
        if (refresh) repository.invalidateCatalog()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val dogs = withTimeoutOrNull(8_000) {
                    coroutineScope {
                        val catalog = async { repository.getDogs() }
                        val group = section?.let { selected -> ExploreRepository().getGroups(selected).find { it.id == groupId } }
                        catalog.await().filter { section == null || group?.contains(it) == true }
                    }
                } ?: error("Tải danh sách quá lâu. Kéo xuống để thử lại.")
                sourceDog = dogs.firstOrNull { it.id == sourceDogId }
                val (links, traits) = loadTraits(dogs)
                traitIdsByDog = links.mapValues { (_, dogTraits) -> dogTraits.map { it.traitId }.toSet() }
                allRows = dogs.filterNot { it.id == sourceDog?.id }.map { dog ->
                    SuggestedDogRow(
                        dog = dog,
                        traits = traitIdsByDog[dog.id].orEmpty()
                            .mapNotNull { traits[it]?.name }
                            .take(3)
                    )
                }
                hasLoaded = true
                publish(finished = true)
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

    private suspend fun loadTraits(dogs: List<Dog>): Pair<Map<String, List<DogTrait>>, Map<String, Trait>> {
        if (dogs.isEmpty()) return emptyMap<String, List<DogTrait>>() to emptyMap()
        val loaded = try {
            withTimeoutOrNull(3_000) {
                coroutineScope {
                    val labels = async { repository.getTraits().associateBy { it.id } }
                    repository.getDogTraitsByDog(dogs.map { it.id }.toSet()) to labels.await()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return loaded ?: (emptyMap<String, List<DogTrait>>() to emptyMap())
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

    private fun publish(finished: Boolean = false) {
        if (allRows.isEmpty() && _state.value.isLoading && !finished) return
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
            ).plus(dog.searchAliases).any { it.contains(current.query, ignoreCase = true) }
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
        _state.value = current.copy(isLoading = current.isLoading && !finished, rows = rows, error = null)
    }

    private fun normalizeOrigin(value: String?): String =
        value.orEmpty().substringBefore("(").trim().lowercase(Locale.getDefault())
}
