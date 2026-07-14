package com.health.nutriscan.data.repository

import com.health.nutriscan.data.model.*
import com.health.nutriscan.data.remote.RetrofitInstance

class NutriCheckRepository {
    private val api = RetrofitInstance.api

    suspend fun createUser(name: String, email: String): Result<UserResponse> = runCatching {
        api.createUser(UserRequest(name, email))
    }

    suspend fun submitScan(userId: Long, name: String, cat: ProductCategory, type: ProductType, text: String): Result<ScanResponse> = runCatching {
        api.submitScan(ScanRequest(userId, name, cat, type, text))
    }

    suspend fun getUserScans(userId: Long): Result<List<ScanSummary>> = runCatching {
        api.getUserScans(userId)
    }
}