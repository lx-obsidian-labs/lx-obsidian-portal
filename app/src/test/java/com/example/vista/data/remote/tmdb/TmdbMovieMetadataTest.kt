package com.example.vista.data.remote.tmdb

import org.junit.Assert.*
import org.junit.Test

class TmdbMovieMetadataTest {
    @Test fun releaseStatusAndCompanyAreNotMisrepresentedAsCertificationOrDirector() {
        val movie = TmdbMovieDetail(id = 1, title = "Fixture", status = "Released", adult = true).toDomainMovie()
        assertNull(movie.ageRating)
        assertNull(movie.director)
        assertTrue(movie.isAdult)
    }
}
