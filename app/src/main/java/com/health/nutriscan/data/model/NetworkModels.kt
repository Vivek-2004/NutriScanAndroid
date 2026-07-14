package com.health.nutriscan.data.model

enum class ProductCategory {
    FOOD, BEVERAGE, COSMETIC, PHARMACEUTICAL
}

enum class ProductType {
    SOLID, LIQUID, SEMI_SOLID
}

data class UserRequest(
    val name: String,
    val email: String
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String
)

data class ScanRequest(
    val userId: Long,
    val productName: String,
    val category: ProductCategory,
    val productType: ProductType,
    val ingredientsText: String
)

data class IngredientAnalysis(
    val name: String,
    val safetyRating: String, // e.g., "SAFE", "MODERATE", "HAZARDOUS"
    val risks: String,
    val description: String
)

data class ScanResponse(
    val id: Long,
    val productName: String,
    val category: ProductCategory,
    val productType: ProductType,
    val overallScore: Int, // Health rating from 0 to 100
    val ingredientsAnalysis: List<IngredientAnalysis>,
    val recommendations: String
)

data class ScanSummary(
    val id: Long,
    val productName: String,
    val overallScore: Int,
    val category: ProductCategory
)