package com.health.nutriscan.data.repository

import android.util.Log
import com.google.gson.Gson
import com.health.nutriscan.data.model.ErrorResponse
import com.health.nutriscan.data.model.HistoryItem
import com.health.nutriscan.data.model.ScanRequest
import com.health.nutriscan.data.model.ScanResponse
import com.health.nutriscan.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NutriCheckRepository {
    private val apiService = RetrofitInstance.api
    private val gson = Gson()

    suspend fun scanIngredients(ingredientsText: String): Result<ScanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ScanRequest(ingredientsText)
                Log.e("Vivek", request.toString())
                val response = apiService.scanIngredients(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = try {
                        val errorBodyString = response.errorBody()?.string()
                        val parsedError = gson.fromJson(errorBodyString, ErrorResponse::class.java)
                        parsedError.message
                    } catch (e: Exception) {
                        "Error Code: ${response.code()} - ${response.message()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getHistory(): Result<List<HistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getScanHistory()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = try {
                        val errorBodyString = response.errorBody()?.string()
                        val parsedError = gson.fromJson(errorBodyString, ErrorResponse::class.java)
                        parsedError.message
                    } catch (e: Exception) {
                        "Error Code: ${response.code()} - ${response.message()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}