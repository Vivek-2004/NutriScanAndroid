package com.health.nutriscan.data.remote

import com.health.nutriscan.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NutriCheckApiService {

    @POST("api/users")
    suspend fun createUser(@Body request: UserRequest): UserResponse

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): UserResponse

    @POST("api/scans")
    suspend fun submitScan(@Body request: ScanRequest): ScanResponse

    @GET("api/scans/user/{userId}")
    suspend fun getUserScans(@Path("userId") userId: Long): List<ScanSummary>

    @GET("api/scans/{id}")
    suspend fun getScanDetails(@Path("id") id: Long): ScanResponse
}