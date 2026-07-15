package com.health.nutriscan.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://10.103.67.44:8080/"

    // Configure OkHttpClient with a 45-second timeout to safely accommodate
    // the 30-second backend processing time plus network latency.
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val api: NutriCheckApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Attach the custom client here
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NutriCheckApiService::class.java)
    }
}