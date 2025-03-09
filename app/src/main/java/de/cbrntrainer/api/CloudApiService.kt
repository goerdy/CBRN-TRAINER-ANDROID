package de.cbrntrainer.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CloudApiService {
    @GET("API.php")
    suspend fun getDeviceData(@Query("session_id") sessionId: String): Response<ApiResponse>
} 