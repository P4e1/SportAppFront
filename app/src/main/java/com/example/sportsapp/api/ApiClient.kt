package com.example.sportsapp.api

import android.content.Context
import com.example.sportsapp.managers.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val apiService: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val tokenManager = TokenManager(appContext)
                val token = tokenManager.getToken()
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")

                // Не добавлять Authorization для /api/register/
                if (!original.url.encodedPath.endsWith("api/register/")) {
                    if (!token.isNullOrEmpty()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                }

                val request = requestBuilder
                    .method(original.method, original.body)
                    .build()

                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}