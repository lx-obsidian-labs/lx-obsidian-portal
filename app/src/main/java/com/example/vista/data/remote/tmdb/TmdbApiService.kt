package com.example.vista.data.remote.tmdb

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
    }
    
    // Movies
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): TmdbMovieDetail
    
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int
    ): TmdbCastResponse
    
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("movie/{movie_id}/recommendations")
    suspend fun getRecommendedMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    // TV Shows
    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
    
    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
    
    @GET("tv/{series_id}")
    suspend fun getTvShowDetails(
        @Path("series_id") seriesId: Int,
        @Query("append_to_response") appendToResponse: String = "external_ids"
    ): TmdbTvShowDetail
    
    @GET("tv/{series_id}/credits")
    suspend fun getTvShowCredits(
        @Path("series_id") seriesId: Int
    ): TmdbCastResponse
    
    @GET("tv/{series_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("series_id") seriesId: Int,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
    
    @GET("tv/{series_id}/recommendations")
    suspend fun getRecommendedTvShows(
        @Path("series_id") seriesId: Int,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
    
    // Seasons & Episodes
    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("series_id") seriesId: Int,
        @Path("season_number") seasonNumber: Int
    ): TmdbSeasonResponse
    
    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getEpisodes(
        @Path("series_id") seriesId: Int,
        @Path("season_number") seasonNumber: Int
    ): TmdbEpisodeResponse
    
    // Search
    @GET("search/multi")
    suspend fun multiSearch(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMultiSearchResult>
    
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>

    @GET("search/person")
    suspend fun searchPeople(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbPersonSearchResult>
    
    // Genres
    @GET("genre/movie/list")
    suspend fun getMovieGenres(): TmdbGenreResponse
    
    @GET("genre/tv/list")
    suspend fun getTvGenres(): TmdbGenreResponse
    
    // Trending
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>
    
    @GET("trending/tv/week")
    suspend fun getTrendingTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
    
    // Videos/Trailers
    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbVideosResponse
    
    @GET("tv/{series_id}/videos")
    suspend fun getTvShowVideos(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbVideosResponse
    
    // Person
    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int
    ): TmdbPersonDetail
    
    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovieCredits(
        @Path("person_id") personId: Int
    ): TmdbPersonMovieCreditsResponse
    
    @GET("person/{person_id}/tv_credits")
    suspend fun getPersonTvCredits(
        @Path("person_id") personId: Int
    ): TmdbPersonTvCreditsResponse

    // Find by external ID
    @GET("find/{external_id}")
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("external_source") externalSource: String = "imdb_id"
    ): TmdbFindResponse

    // Discover
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreId: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("with_genres") genreId: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbTvShow>
}

data class TmdbFindResponse(
    @SerializedName("movie_results") val movieResults: List<TmdbMovie>,
    @SerializedName("tv_results") val tvResults: List<TmdbTvShow>,
    @SerializedName("person_results") val personResults: List<TmdbPersonDetail>?
)

data class TmdbGenreResponse(
    val genres: List<TmdbGenre>
)

/**
 * A `/search/person` hit. Kept separate from [TmdbCastMember] because a search result has no
 * character credit.
 */
data class TmdbPersonSearchResult(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for_department") val department: String? = null
) {
    fun toDomainPerson(): com.example.vista.domain.model.Person {
        return com.example.vista.domain.model.Person(
            id = id.toString(),
            name = name,
            profileUrl = TmdbImageUtils.profileUrl(profilePath),
            character = null,
            department = department
        )
    }
}

data class TmdbVideosResponse(
    val id: Int,
    val results: List<TmdbVideo>
)
