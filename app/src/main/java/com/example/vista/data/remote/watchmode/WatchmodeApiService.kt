package com.example.vista.data.remote.watchmode

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WatchmodeApiService {

    companion object {
        const val BASE_URL = "https://api.watchmode.com/v1/"
    }

    @GET("title/{title_id}/sources/")
    suspend fun getSources(
        @Path("title_id") titleId: String,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "US"
    ): List<WatchmodeSource>

    @GET("title/{title_id}/details/")
    suspend fun getDetails(
        @Path("title_id") titleId: String,
        @Query("apiKey") apiKey: String
    ): WatchmodeDetails

    @GET("search/")
    suspend fun search(
        @Query("apiKey") apiKey: String,
        @Query("search_value") query: String,
        @Query("search_field") searchField: String = "name"
    ): Any // Search response structure varies
}
