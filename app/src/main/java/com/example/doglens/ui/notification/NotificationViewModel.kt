package com.example.doglens.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.model.RecognitionResult
import com.example.doglens.data.repository.FirestoreAccountData
import com.example.doglens.utils.TimeUtils
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class NotificationType { RECOGNITION, FAVORITE, ARTICLE, UPDATE }
enum class NotificationFilter { ALL, RECOGNITION, UPDATE }

data class DogLensNotification(
    val id: String,
    val section: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean,
    val recognition: RecognitionResult? = null
)

sealed interface NotificationRow {
    data class Header(val title: String) : NotificationRow
    data class Item(val notification: DogLensNotification) : NotificationRow
}

data class NotificationUiState(
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val rows: List<NotificationRow> = emptyList(),
    val error: String? = null
)

class NotificationViewModel : ViewModel() {
    private val account = FirestoreAccountData()
    private val feed = account.collection("notifications")
    private var notifications = emptyList<DogLensNotification>()
    private var recognitionIds = emptySet<String>()
    private var previews = emptyMap<String, RecognitionResult>()
    private var previewJob: Job? = null
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    private val feedQuery = feed?.orderBy("createdAt", Query.Direction.DESCENDING)?.limit(100)
    private val listener = feedQuery?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _state.value = _state.value.copy(error = "Không tải được thông báo.")
                return@addSnapshotListener
            }
            if (snapshot != null) updateFeed(snapshot)
        }

    suspend fun refresh() {
        val query = feedQuery ?: return
        val snapshot = query.get(Source.SERVER).await()
        updateFeed(snapshot, forcePreviews = true)
        previewJob?.join()
    }

    private fun updateFeed(snapshot: QuerySnapshot, forcePreviews: Boolean = false) {
            notifications = snapshot.documents.mapNotNull { document ->
                val type = NotificationType.entries.firstOrNull { it.name == document.getString("type") }
                    ?: return@mapNotNull null
                val date = document.getTimestamp("createdAt")?.toDate()?.time ?: return@mapNotNull null
                val section = if (TimeUtils.startOfDay(date) == TimeUtils.startOfDay(System.currentTimeMillis()))
                    "Hôm nay" else "Trước đó"
                DogLensNotification(document.id, section, type, document.getString("title").orEmpty(),
                    document.getString("message").orEmpty(), TimeUtils.timeLabel(date),
                    document.getBoolean("isRead") == true)
            }
            publish()
            val ids = notifications.filter { it.type == NotificationType.RECOGNITION }.map { it.id }.toSet()
            if (ids == recognitionIds && !forcePreviews) return
            recognitionIds = ids
            previewJob?.cancel()
            previewJob = viewModelScope.launch {
                try {
                    previews = AppContainer.recognitionRepository.getPreviews(ids)
                    publish()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    recognitionIds = emptySet()
                }
            }
    }

    fun setFilter(filter: NotificationFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
        publish()
    }

    fun markRead(id: String) {
        if (notifications.any { it.id == id && !it.isRead }) mark(listOf(id))
    }

    fun markAllRead() {
        mark(notifications.filterNot { it.isRead }.map { it.id })
    }

    private fun mark(ids: List<String>) {
        val collection = feed ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val batch = account.db.batch()
                ids.forEach { batch.update(collection.document(it), "isRead", true) }
                batch.commit().await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = "Không lưu được trạng thái thông báo.")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    private fun publish() {
        val filter = _state.value.selectedFilter
        val filtered = notifications.filter {
            when (filter) {
                NotificationFilter.ALL -> true
                NotificationFilter.RECOGNITION -> it.type == NotificationType.RECOGNITION
                NotificationFilter.UPDATE -> it.type != NotificationType.RECOGNITION
            }
        }
        val rows = buildList {
            filtered.groupBy { it.section }.forEach { (section, items) ->
                add(NotificationRow.Header(section))
                items.forEach {
                    add(NotificationRow.Item(it.copy(recognition = previews[it.id])))
                }
            }
        }
        _state.value = _state.value.copy(rows = rows)
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
