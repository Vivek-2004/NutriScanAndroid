package com.health.nutriscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.nutriscan.data.model.*
import com.health.nutriscan.data.repository.NutriCheckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class NutriCheckViewModel : ViewModel() {
    private val repository = NutriCheckRepository()

    // Explicitly typed StateFlows to prevent compiler type inference errors
    private val _historyState = MutableStateFlow<UiState<List<ScanSummary>>>(UiState.Idle)
    val historyState: StateFlow<UiState<List<ScanSummary>>> = _historyState

    private val _scanResultState = MutableStateFlow<UiState<ScanResponse>>(UiState.Idle)
    val scanResultState: StateFlow<UiState<ScanResponse>> = _scanResultState

    // Hardcode dummy user ID to bypass login functionality completely
    private val dummyUserId = 1L

    fun loadScanHistory() {
        viewModelScope.launch {
            _historyState.value = UiState.Loading
            repository.getUserScans(dummyUserId)
                .onSuccess { _historyState.value = UiState.Success(it) }
                .onFailure { _historyState.value = UiState.Error(it.localizedMessage ?: "Failed to load history") }
        }
    }

    fun analyzeIngredients(productName: String, category: ProductCategory, type: ProductType, rawText: String) {
        viewModelScope.launch {
            _scanResultState.value = UiState.Loading

            repository.submitScan(dummyUserId, productName, category, type, rawText)
                .onSuccess {
                    _scanResultState.value = UiState.Success(it)
                    loadScanHistory() // Automatically refresh history after a new scan
                }
                .onFailure {
                    _scanResultState.value = UiState.Error(it.localizedMessage ?: "Analysis failed")
                }
        }
    }

    fun clearScanResult() {
        _scanResultState.value = UiState.Idle
    }
}