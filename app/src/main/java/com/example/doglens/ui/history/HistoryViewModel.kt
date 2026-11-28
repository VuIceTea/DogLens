package com.example.doglens.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.RecognitionHistoryItem
import com.example.doglens.data.repository.DogRepository
import com.example.doglens.data.repository.FavoriteRepository
import com.example.doglens.data.repository.RecognitionRepository
import com.example.doglens.utils.TimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Bộ lọc thời gian (chỉ UI)
enum class HistoryFilter { ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS }

// 1 dòng trong danh sách: tiêu đề ngày hoặc 1 lần nhận diện (chỉ UI)
sealed interface HistoryRow {
    data class Header(val title: String, val count: Int) : HistoryRow
    data class Entry(val item: RecognitionHistoryItem, val timeLabel: String) : HistoryRow
}

data class HistoryUiState(
    val loading: Boolean = true,
    val rows: List<HistoryRow> = emptyList(),
    val totalCount: Int = 0,          // tổng số bản ghi (chưa lọc)
    val filter: HistoryFilter = HistoryFilter.ALL,
    val query: String = "",
    val error: String? = null
)

class HistoryViewModel(
    private val recognitionRepository: RecognitionRepository = AppContainer.recognitionRepository,
    private val dogRepository: DogRepository = AppContainer.dogRepository,
    private val favoriteRepository: FavoriteRepository = AppContainer.favoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    // Toàn bộ lịch sử đã ghép với Dog + trạng thái yêu thích
    private var allItems: List<RecognitionHistoryItem> = emptyList()
    private var refreshJob: Job? = null
    private val pendingFavorites = mutableMapOf<String, FavoriteChange>()
    private data class FavoriteChange(val saved: Boolean)
    private val pendingDeletes = mutableSetOf<String>()
    private var mutationRevision = 0
    private var clearingHistory = false

    // Gọi mỗi lần vào màn (kể cả quay lại từ màn Kết quả) để cập nhật tim yêu thích
    fun refresh() {
        if (refreshJob?.isActive == true) return
        val revision = mutationRevision
        _state.update { it.copy(loading = true) }
        refreshJob = viewModelScope.launch {
            try {
                val loaded = loadItems()
                if (revision == mutationRevision) allItems = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Không tải được lịch sử nhận diện.") }
            }
            _state.update { it.copy(loading = false) }
            publish()
        }
    }

    // Ghép RecognitionResult -> Dog (dogId) và FavoriteDog (dogId)
    private suspend fun loadItems(): List<RecognitionHistoryItem> = coroutineScope {
        val dogs = async { dogRepository.getDogs().associateBy { it.id } }
        val favorites = async {
            try {
                favoriteRepository.getFavoriteDogIds()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                allItems.filter { it.isFavorite }.map { it.dog.id }.toSet()
            }
        }
        val history = recognitionRepository.getHistory()
        val dogsById = dogs.await()
        val favoriteDogIds = favorites.await()
        history
            .sortedByDescending { TimeUtils.parseIso(it.recognizedAt) }
            .mapNotNull { result ->
                val dog = dogsById[result.dogId] ?: return@mapNotNull null
                RecognitionHistoryItem(result, dog, dog.id in favoriteDogIds)
            }
    }

    fun setFilter(filter: HistoryFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        publish()
    }

    fun setQuery(query: String) {
        val trimmed = query.trim()
        if (_state.value.query == trimmed) return
        _state.update { it.copy(query = trimmed) }
        publish()
    }

    fun toggleFavorite(item: RecognitionHistoryItem) {
        val id = item.dog.id
        val saved = !item.isFavorite
        val change = FavoriteChange(saved)
        pendingFavorites[id] = change
        publish()
        viewModelScope.launch {
            try {
                if (saved) favoriteRepository.addFavorite(id) else favoriteRepository.removeFavorite(id)
                mutationRevision++
                allItems = allItems.map { if (it.dog.id == id) it.copy(isFavorite = saved) else it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (pendingFavorites[id] === change) {
                    _state.update { it.copy(error = "Không cập nhật được giống chó yêu thích.") }
                }
            } finally {
                if (pendingFavorites[id] === change) pendingFavorites.remove(id)
                publish()
            }
        }
    }

    fun delete(item: RecognitionHistoryItem) {
        val id = item.result.id
        if (!pendingDeletes.add(id)) return
        publish()
        viewModelScope.launch {
            try {
                recognitionRepository.delete(id)
                mutationRevision++
                allItems = allItems.filterNot { it.result.id == id }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = "Không xóa được kết quả nhận diện.") }
            } finally {
                pendingDeletes.remove(id)
                publish()
            }
        }
    }

    fun clearAll() {
        if (clearingHistory) return
        clearingHistory = true
        publish()
        updateHistory("Không xóa được toàn bộ lịch sử. Hãy kiểm tra và thử lại.") {
            try {
                recognitionRepository.clearHistory()
                mutationRevision++
                allItems = emptyList()
            } finally {
                clearingHistory = false
                publish()
            }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    private fun updateHistory(error: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                refresh()
                _state.update { it.copy(error = error) }
            }
        }
    }

    // Lọc + tìm kiếm + gom nhóm theo ngày -> danh sách dòng cho RecyclerView
    private fun publish() {
        val current = _state.value
        val todayStart = TimeUtils.startOfDay(System.currentTimeMillis())
        val minTime = when (current.filter) {
            HistoryFilter.ALL -> Long.MIN_VALUE
            HistoryFilter.TODAY -> todayStart
            HistoryFilter.LAST_7_DAYS -> todayStart - 6 * DAY_MILLIS
            HistoryFilter.LAST_30_DAYS -> todayStart - 29 * DAY_MILLIS
        }
        val keyword = current.query.lowercase()

        val visibleItems = (if (clearingHistory) emptyList() else allItems).asSequence().filterNot { it.result.id in pendingDeletes }
            .map { item -> pendingFavorites[item.dog.id]?.let { item.copy(isFavorite = it.saved) } ?: item }.toList()
        val filtered = visibleItems.filter { item ->
            val time = TimeUtils.parseIso(item.result.recognizedAt)
            time >= minTime && (keyword.isEmpty() || item.dog.name.lowercase().contains(keyword))
        }

        val rows = mutableListOf<HistoryRow>()
        filtered
            .groupBy { TimeUtils.parseIso(it.result.recognizedAt).takeIf { time -> time > 0 }?.let(TimeUtils::startOfDay) }
            .forEach { (dayStart, items) ->
                rows += HistoryRow.Header(dayStart?.let(TimeUtils::dayLabel) ?: "Chưa rõ ngày", items.size)
                items.forEach { item ->
                    rows += HistoryRow.Entry(
                        item = item,
                        timeLabel = TimeUtils.timeLabel(TimeUtils.parseIso(item.result.recognizedAt))
                    )
                }
            }

        _state.value = current.copy(rows = rows, totalCount = visibleItems.size)
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
