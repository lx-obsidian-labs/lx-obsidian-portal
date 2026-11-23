package com.example.vista.feedback

data class PollOption(val id: String, val label: String)
data class PollQuestion(val id: String, val title: String, val options: List<PollOption>)

object FeedbackPoll {
    const val ID = "experience_1_0_11"
    const val MAX_COMMENT = 500
    val questions = listOf(
        PollQuestion("feeling", "How are you feeling about Vista?", listOf(
            PollOption("love", "I love it"), PollOption("good", "Mostly good"),
            PollOption("mixed", "Mixed feelings"), PollOption("frustrated", "Frustrated"),
            PollOption("new", "Still getting to know it")
        )),
        PollQuestion("friction", "What's getting in your way most?", listOf(
            PollOption("none", "Nothing right now"), PollOption("playback", "Playback or buffering"),
            PollOption("ads", "Ads in the embedded player"), PollOption("catalog", "Missing or empty movie data"),
            PollOption("navigation", "Finding my way around"), PollOption("downloads", "Downloads"),
            PollOption("other", "Something else")
        )),
        PollQuestion("priority", "What should we improve next?", listOf(
            PollOption("reliability", "More reliable playback"), PollOption("discovery", "Better movie discovery"),
            PollOption("offline", "Offline downloads"), PollOption("tv", "The TV experience"),
            PollOption("assistant", "The AI companion"), PollOption("design", "Design and ease of use")
        )),
        PollQuestion("discovery", "How did you first hear about Vista?", listOf(
            PollOption("friend", "A friend or family member"), PollOption("whatsapp", "WhatsApp"),
            PollOption("social", "Social media"), PollOption("search", "An online search"),
            PollOption("website", "The Vista website"), PollOption("other", "Somewhere else"),
            PollOption("skip", "Prefer not to say")
        ))
    )

    fun valid(answers: Map<String, String>, comment: String): Boolean =
        answers.keys == questions.map { it.id }.toSet() &&
            questions.all { question -> question.options.any { it.id == answers[question.id] } } &&
            comment.length <= MAX_COMMENT
}
