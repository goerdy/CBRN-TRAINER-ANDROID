package de.cbrntrainer.repository

import android.content.Context
import android.util.Log
import de.cbrntrainer.api.CloudApiService
import de.cbrntrainer.api.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CloudRepository private constructor(context: Context) {
    private val PREFS_NAME = "CloudSettings"
    private val URL_KEY = "base_url"
    private val DEFAULT_URL = "https://cbrn-trainer.de/c"
    
    private var apiService: CloudApiService? = null
    private var lastResponse: ApiResponse? = null
    
    init {
        setupApiService(context)
    }
    
    private fun setupApiService(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString(URL_KEY, DEFAULT_URL) ?: DEFAULT_URL
        
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(CloudApiService::class.java)
    }
    
    fun getDeviceDataFlow(sessionId: String, intervalMs: Long = 200L): Flow<Result<ApiResponse>> = flow {
        while(true) {
            try {
                val response = apiService?.getDeviceData(sessionId)
                Log.d("CloudRepository", "Raw response: ${response?.raw()?.toString()}")
                
                if (response?.isSuccessful == true) {
                    response.body()?.let { data ->
                        lastResponse = data
                        emit(Result.success(data))
                    } ?: emit(Result.failure(Exception("Empty response body")))
                } else {
                    emit(Result.failure(Exception("API call failed: ${response?.code()} - ${response?.errorBody()?.string()}")))
                }
            } catch (e: Exception) {
                Log.e("CloudRepository", "Error fetching data", e)
                emit(Result.failure(e))
            }
            kotlinx.coroutines.delay(intervalMs)
        }
    }
    
    fun getLastData(): ApiResponse? = lastResponse
    
    companion object {
        @Volatile
        private var instance: CloudRepository? = null
        
        fun getInstance(context: Context): CloudRepository {
            return instance ?: synchronized(this) {
                instance ?: CloudRepository(context).also { instance = it }
            }
        }
    }
} 