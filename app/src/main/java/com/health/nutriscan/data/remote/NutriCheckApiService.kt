package com.health.nutriscan.data.remote

import com.health.nutriscan.data.model.HistoryItem
import com.health.nutriscan.data.model.ScanRequest
import com.health.nutriscan.data.model.ScanResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface NutriCheckApiService {
    @POST("api/scan/ingredients")
    suspend fun scanIngredients(@Body request: ScanRequest): Response<ScanResponse>

    @GET("api/scan")
    suspend fun getScanHistory(): Response<List<HistoryItem>>
}