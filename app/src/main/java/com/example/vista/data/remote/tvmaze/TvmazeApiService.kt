package com.example.vista.data.remote.tvmaze

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvmazeApiService {
    
    companion object {
        const val BASE_URL = "https://api.tvmaze.com/"
    }
    
    // Schedule
    @GET("schedule")
    suspend fun getSchedule(
        @Query("country") country: String = "US",
        @Query("date") date: String? = null
    ): List<TvmazeScheduleEpisode>
    
    @GET("schedule/full")
    suspend fun getFullSchedule(): List<TvmazeScheduleEpisode>
    
    // Shows
    @GET("shows")
    suspend fun getShows(
        @Query("page") page: Int = 1
    ): List<TvmazeShow>
    
    @GET("search/shows")
    suspend fun searchShows(
        @Query("q") query: String
    ): List<TvmazeSearchResult>
    
    @GET("singlesearch/shows")
    suspend fun singleSearchShow(
        @Query("q") query: String,
        @Query("embed") embed: String? = null
    ): TvmazeShow
    
    @GET("shows/{id}")
    suspend fun getShow(
        @Path("id") id: Int,
        @Query("embed") embed: String? = null
    ): TvmazeShow
    
    // Episodes
    @GET("shows/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") showId: Int
    ): List<TvmazeEpisode>
    
    @GET("shows/{id}/episodebynumber")
    suspend fun getEpisodeByNumber(
        @Path("id") showId: Int,
        @Query("season") season: Int,
        @Query("number") number: Int
    ): TvmazeEpisode
    
    @GET("shows/{id}/episodesbydate")
    suspend fun getEpisodesByDate(
        @Path("id") showId: Int,
        @Query("date") date: String
    ): List<TvmazeEpisode>
    
    // People
    @GET("people/{id}")
    suspend fun getPerson(
        @Path("id") id: Int
    ): TvmazeShow // Placeholder - TVmaze person model would be different
}
