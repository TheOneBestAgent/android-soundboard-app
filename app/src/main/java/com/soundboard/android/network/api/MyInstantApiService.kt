package com.soundboard.android.network.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class MyInstantResponse(
    val id: String,
    val url: String,
    val title: String,
    val mp3: String,
    val description: String?,
    val tags: List<String>?,
    val favorites: Int?,
    val views: Int?,
    val uploader: MyInstantUploader?
)

data class MyInstantUploader(
    val name: String?,
    val profile_url: String?
)

data class MyInstantApiResponse(
    val status: String,
    val author: String,
    val data: List<MyInstantResponse>
)

interface MyInstantApiService {
    @GET("search")
    suspend fun searchSounds(@Query("q") query: String): Response<MyInstantApiResponse>
    
    @GET("trending")
    suspend fun getTrendingSounds(@Query("q") region: String = "us"): Response<MyInstantApiResponse>
    
    @GET("recent")
    suspend fun getRecentSounds(): Response<MyInstantApiResponse>
    
    @GET("best")
    suspend fun getBestSounds(): Response<MyInstantApiResponse>
    
    @GET("detail")
    suspend fun getSoundDetails(@Query("id") soundId: String): Response<MyInstantResponse>
} 