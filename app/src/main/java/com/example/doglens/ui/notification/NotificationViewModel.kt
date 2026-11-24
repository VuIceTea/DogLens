package com.example.doglens.ui.notification

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NotificationType { RECOGNITION, FAVORITE, ARTICLE, UPDATE }
enum class NotificationFilter { ALL, RECOGNITION, UPDATE }

data class DogLensNotification(
    val id: Long,
    val section: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean
)

sealed interface NotificationRow {
    data class Header(val title: String) : NotificationRow
    data class Item(val notification: DogLensNotification) : NotificationRow
}

data class NotificationUiState(
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val rows: List<NotificationRow> = emptyList()
)

class NotificationViewModel : ViewModel() {
    private val notifications = MutableStateFlow(
        listOf(
            DogLensNotification(1, "Hôm nay", NotificationType.RECOGNITION, "Đã nhận diện thành công!", "Ảnh của bạn được nhận diện là Golden Retriever với độ chính xác 96%.", "2 phút trước", false),
            DogLensNotification(2, "Hôm nay", NotificationType.FAVORITE, "Giống chó mới được yêu thích", "Golden Retriever đã được thêm vào danh sách yêu thích.", "1 giờ trước", false),
            DogLensNotification(3, "Trước đó", NotificationType.ARTICLE, "Bài viết mới dành cho bạn", "5 mẹo chăm sóc bộ lông khỏe đẹp cho chó.", "Hôm qua", true),
            DogLensNotification(4, "Trước đó", NotificationType.UPDATE, "Có bản cập nhật mới", "Khám phá trải nghiệm nhận diện nhanh và chính xác hơn.", "2 ngày trước", true)
        )
    )
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init { publish() }

    fun setFilter(filter: NotificationFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
        publish()
    }

    fun markRead(id: Long) {
        notifications.value = notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
        publish()
    }

    fun markAllRead() {
        notifications.value = notifications.value.map { it.copy(isRead = true) }
        publish()
    }

    private fun publish() {
        val filter = _state.value.selectedFilter
        val filtered = notifications.value.filter {
            when (filter) {
                NotificationFilter.ALL -> true
                NotificationFilter.RECOGNITION -> it.type == NotificationType.RECOGNITION
                NotificationFilter.UPDATE -> it.type != NotificationType.RECOGNITION
            }
        }
        val rows = buildList {
            filtered.groupBy { it.section }.forEach { (section, items) ->
                add(NotificationRow.Header(section))
                items.forEach { add(NotificationRow.Item(it)) }
            }
        }
        _state.value = _state.value.copy(rows = rows)
    }
}
