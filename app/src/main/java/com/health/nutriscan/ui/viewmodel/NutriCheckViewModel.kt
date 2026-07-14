package com.health.nutriscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.nutriscan.data.model.HistoryItem
import com.health.nutriscan.data.model.ScanResponse
import com.health.nutriscan.data.repository.NutriCheckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Loading : ScanState()
    data class Success(val response: ScanResponse) : ScanState()
    data class Error(val message: String) : ScanState()
}

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val history: List<HistoryItem>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class NutriCheckViewModel : ViewModel() {
    private val repository = NutriCheckRepository()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    fun processIngredients(extractedText: String) {
        if (extractedText.isBlank()) {
            _scanState.value = ScanState.Error("Could not detect any text. Try again.")
            return
        }

        _scanState.value = ScanState.Loading

        viewModelScope.launch {
            val result = repository.scanIngredients(extractedText)
            result.fold(
                onSuccess = { _scanState.value = ScanState.Success(it) },
                onFailure = { _scanState.value = ScanState.Error(it.message ?: "Failed to connect to backend") }
            )
        }
    }

    fun fetchHistory() {
        _historyState.value = HistoryState.Loading

        viewModelScope.launch {
            val result = repository.getHistory()
            result.fold(
                onSuccess = { _historyState.value = HistoryState.Success(it) },
                onFailure = { _historyState.value = HistoryState.Error(it.message ?: "Failed to fetch history") }
            )
        }
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}