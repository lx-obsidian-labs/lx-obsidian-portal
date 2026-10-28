package com.example.vista.data.remote.shoofaflam

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface ShoofaflamApiService {

    companion object {
        const val BASE_URL = "https://shoofaflam.tv/api/"
    }

    @GET("titles.json")
    suspend fun getAllTitles(): List<ShoofaflamTitle>

    @GET("platforms.json")
    suspend fun getAllPlatforms(): List<ShoofaflamPlatform>

    @GET("stats.json")
    suspend fun getStats(): ShoofaflamStats

    @GET("search/{letter}.json")
    suspend fun searchByLetter(@Path("letter") letter: String): List<ShoofaflamTitle>
}
