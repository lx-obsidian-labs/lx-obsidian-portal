package com.example.vista.feedback

import org.junit.Assert.*
import org.junit.Test

class FeedbackPollTest {
    private fun answers() = FeedbackPoll.questions.associate { it.id to it.options.first().id }
    @Test fun completeAnswersAreAccepted() { assertTrue(FeedbackPoll.valid(answers(), "Honest feedback")) }
    @Test fun missingOrInventedAnswersAreRejected() {
        assertFalse(FeedbackPoll.valid(answers() - "feeling", ""))
        assertFalse(FeedbackPoll.valid(answers() + ("feeling" to "invented"), ""))
        assertFalse(FeedbackPoll.valid(answers() + ("extra" to "value"), ""))
    }
    @Test fun commentsAreBounded() {
        assertTrue(FeedbackPoll.valid(answers(), "x".repeat(500)))
        assertFalse(FeedbackPoll.valid(answers(), "x".repeat(501)))
    }
}
