package com.health.nutriscan.data.model

import com.google.gson.annotations.SerializedName

// --- POST Scan Endpoints ---
data class ScanRequest(
    @SerializedName("ingredients") val ingredients: String,
    @SerializedName("productCategory") val productCategory: String = "FOOD"
)

data class ScanResponse(
    @SerializedName("productName") val productName: String?,
    @SerializedName("results") val results: List<IngredientResult>,
    @SerializedName("safetyScore") val safetyScore: Int,
    @SerializedName("overallAssessment") val overallAssessment: String,
    @SerializedName("warningsFor") val warningsFor: List<String>
)

data class IngredientResult(
    @SerializedName("ingredientName") val ingredientName: String,
    @SerializedName("risk") val risk: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("sideEffects") val sideEffects: List<String>
)

// --- GET History Endpoints ---
data class HistoryItem(
    @SerializedName("scanId") val scanId: Int,
    @SerializedName("productName") val productName: String?,
    @SerializedName("scannedAt") val scannedAt: String,
    @SerializedName("userId") val userId: Int,
    @SerializedName("userName") val userName: String,
    @SerializedName("results") val results: List<HistoryIngredientResult>,
    @SerializedName("summary") val summary: HistorySummary
)

data class HistoryIngredientResult(
    @SerializedName("resultId") val resultId: Int,
    @SerializedName("ingredientName") val ingredientName: String,
    @SerializedName("risk") val risk: String,
    @SerializedName("severity") val severity: String?,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("sideEffects") val sideEffects: String?
)

data class HistorySummary(
    @SerializedName("totalIngredients") val totalIngredients: Int,
    @SerializedName("lowRiskCount") val lowRiskCount: Int,
    @SerializedName("mediumRiskCount") val mediumRiskCount: Int,
    @SerializedName("highRiskCount") val highRiskCount: Int,
    @SerializedName("overallRisk") val overallRisk: String
)

// --- General Error Handling ---
data class ErrorResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String,
    @SerializedName("timestamp") val timestamp: String
)