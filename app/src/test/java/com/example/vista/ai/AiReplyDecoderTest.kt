package com.example.vista.ai

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class AiReplyDecoderTest {
    private fun decode(json: String) = AiReplyDecoder.enrich(AssistantReply("Usable answer", destination = "details/123"), JsonParser.parseString(json).asJsonObject)

    @Test fun malformedOptionalEvidencePreservesTextAndCatalogActions() {
        val reply = decode("""{"sources":{},"followups":[null,123,"Tell me more"],"retrievedAt":"invalid"}""")
        assertEquals("Usable answer", reply.text)
        assertEquals("details/123", reply.destination)
        assertTrue(reply.sources.isEmpty())
        assertEquals(listOf("Tell me more"), reply.followups)
        assertNull(reply.retrievedAt)
    }

    @Test fun verifiedSourceIdsAndDatesSurviveForStoryFollowups() {
        val reply = decode("""{"sources":[{"id":"article-two","title":"A series announcement","url":"https://publisher.org/story","publisher":"Publisher","publishedAt":"2026-11-17T12:00:00Z"}],"newsFeed":"za"}""")
        assertEquals("article-two", reply.sources.single().id)
        assertEquals("za", reply.newsFeed)
    }

    @Test fun sourceActionsRejectCredentialUrlsAndLocalAddresses() {
        for (url in listOf("javascript:alert(1)", "https://127.0.0.1/story", "https://user:pass@publisher.org/story", "https://host.local/story")) {
            val reply = decode("""{"sources":[{"title":"Story","url":"$url","publishedAt":"2026-11-17T12:00:00Z"}]}""")
            assertTrue(reply.sources.isEmpty())
        }
    }
}
