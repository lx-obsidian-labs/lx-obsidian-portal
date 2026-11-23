package com.example.vista.data.remote.playback

import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.StreamType
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class BackupMovieSourceTest {
    @Test fun `movie fallback uses documented route and encoded key`() {
        val source = BackupMovieSource.create("597", MediaType.MOVIE, "test+key&value")!!
        val url = source.url.toHttpUrl()
        assertEquals("/embed/movie/597", url.encodedPath)
        assertEquals("test+key&value", url.queryParameter("apikey"))
        assertEquals(1, url.querySize)
        assertEquals(StreamType.OTHER, source.type)
        assertTrue(EmbedOrigins.allowsTopLevel(source.url))
        assertFalse(EmbedOrigins.allowsTopLevel("https://api.codespecters.com.evil.example/embed/movie/597"))
    }

    @Test fun `fallback skips series and incompatible or missing identifiers`() {
        assertNull(BackupMovieSource.create("597", MediaType.SERIES, "test-key"))
        for (id in listOf(null, "tt0120338", "movie-slug", "0", "-1")) {
            assertNull(BackupMovieSource.create(id, MediaType.MOVIE, "test-key"))
        }
    }

    @Test fun `missing or placeholder keys disable fallback`() {
        for (key in listOf("", "  ", "backup_movies_api_key_here", "your_nexstream_api_key_here")) {
            assertNull(BackupMovieSource.create("597", MediaType.MOVIE, key))
        }
    }
}
