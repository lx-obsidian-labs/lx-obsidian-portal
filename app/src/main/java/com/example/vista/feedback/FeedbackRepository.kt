package com.example.vista.feedback

import com.example.vista.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun submit(answers: Map<String, String>, comment: String, quoteAllowed: Boolean) {
        require(FeedbackPoll.valid(answers, comment)) { "Please answer each question before sending." }
        val uid = auth.currentUser?.uid ?: error("Sign in to send your feedback.")
        val document = firestore.collection("viewers").document(uid)
        firestore.runTransaction { transaction ->
            check(auth.currentUser?.uid == uid) { "Your account changed. Please reopen the poll." }
            val existing = transaction.get(document)
            // A stable campaign field makes retries idempotent and avoids duplicate votes.
            if (existing.get("feedbackPolls.${FeedbackPoll.ID}") == null) {
                transaction.set(document, mapOf("feedbackPolls" to mapOf(FeedbackPoll.ID to mapOf(
                    "answers" to answers,
                    "comment" to comment.trim(),
                    "quoteAllowed" to (quoteAllowed && comment.isNotBlank()),
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "submittedAt" to FieldValue.serverTimestamp()
                ))), SetOptions.merge())
            }
        }.await()
    }
}
