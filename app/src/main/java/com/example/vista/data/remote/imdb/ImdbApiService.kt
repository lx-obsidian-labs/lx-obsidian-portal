package com.example.vista.data.remote.imdb

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface ImdbApiService {
    
    companion object {
        const val BASE_URL = "https://imdb.iamidiotareyoutoo.com/"
    }
    
    @GET("search")
    suspend fun search(@Query("q") query: String): ImdbSearchResponse
}

data class ImdbSearchResponse(
    val description: List<ImdbMovieItem>?,
    val ok: Boolean?
)

data class ImdbMovieItem(
    @SerializedName("#TITLE")
    val title: String?,
    @SerializedName("#YEAR")
    val year: Int?,
    @SerializedName("#IMDB_ID")
    val imdbId: String?,
    @SerializedName("#RANK")
    val rank: Int?,
    @SerializedName("#ACTORS")
    val actors: String?,
    @SerializedName("#AKA")
    val aka: String?,
    @SerializedName("#IMDB_URL")
    val imdbUrl: String?,
    @SerializedName("#IMG_POSTER")
    val posterUrl: String?,
    val photo_width: Int?,
    val photo_height: Int?
)
