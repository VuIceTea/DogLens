package com.example.doglens.ui.recognition

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doglens.data.AppContainer
import com.example.doglens.data.repository.RecognitionRepository
import com.example.doglens.data.repository.RecognitionFailedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Trạng thái màn hình (chỉ UI, không phải model DB)
sealed interface RecognitionUiState {
    data class Running(val percent: Int) : RecognitionUiState
    data class Success(val recognitionId: String) : RecognitionUiState
    data class Failed(val dogDetected: Boolean) : RecognitionUiState
    data class Unavailable(val message: String) : RecognitionUiState
}

class RecognitionViewModel(
    private val recognitionRepository: RecognitionRepository = AppContainer.recognitionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Running(0))
    val state: StateFlow<RecognitionUiState> = _state.asStateFlow()

    private var started = false

    // Chỉ chạy 1 lần: xoay màn hình không chạy lại từ 0%
    fun start(imageUri: Uri) {
        if (started) return
        started = true

        viewModelScope.launch {
            try {
                val result = recognitionRepository.recognize(imageUri) { percent ->
                    _state.value = RecognitionUiState.Running(percent)
                }
                _state.value = RecognitionUiState.Success(result.id)
            } catch (e: CancellationException) {
                throw e // bấm Back -> huỷ bình thường
            } catch (e: RecognitionFailedException) {
                _state.value = RecognitionUiState.Failed(e.dogDetected)
            } catch (e: IllegalStateException) {
                _state.value = RecognitionUiState.Unavailable(e.message ?: "Không thể xử lý ảnh.")
            } catch (_: Exception) {
                _state.value = RecognitionUiState.Unavailable("Không thể kết nối hoặc lưu kết quả nhận diện.")
            }
        }
    }
}
