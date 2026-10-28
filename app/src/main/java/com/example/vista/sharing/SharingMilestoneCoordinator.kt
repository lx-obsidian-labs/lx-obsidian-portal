package com.example.vista.sharing

import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.MilestoneEngine
import com.example.vista.domain.model.SharePromptPolicy
import com.example.vista.domain.model.SharingMilestone
import com.example.vista.domain.model.WatchStatsCalculator
import com.example.vista.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether to offer a share prompt, and delivers it.
 *
 * All of the judgement lives in the pure [SharePromptPolicy] and [MilestoneEngine]; this class only
 * gathers inputs and performs the effect. That split is deliberate - segmentation and frequency
 * capping are the two rules most likely to be quietly broken by a later change, and they are the
 * two covered by unit tests.
 */
@Singleton
class SharingMilestoneCoordinator @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val historyRepository: HistoryRepository,
    private val notifier: SharingMilestoneNotifier
) {

    /**
     * @param notifyInsteadOfOpening true when the app has just cold started, so the prompt should
     * arrive as a notification the viewer taps, rather than yanking them onto a screen they were
     * already using.
     */
    suspend fun evaluate(notifyInsteadOfOpening: Boolean): SharingMilestone? {
        val enabled = preferencesManager.sharePromptsEnabled.first()
        val launches = preferencesManager.launchCount()
        val celebrated = preferencesManager.celebratedMilestones()
        val (count, month, lastPromptAt) = preferencesManager.sharePromptHistory()

        val stats = WatchStatsCalculator.compute(historyRepository.getWatchHistory().first())

        val verdict = SharePromptPolicy.evaluate(
            enabled = enabled,
            launches = launches,
            titlesStarted = stats.titlesStarted,
            completedTitles = stats.titlesCompleted,
            promptsThisMonth = count,
            monthKey = month,
            lastPromptAt = lastPromptAt
        )
        if (!verdict.allowed) return null

        val milestone = MilestoneEngine.detect(stats, celebrated) ?: return null

        // In-app the screen is opened directly, so nothing is "delivered" yet - the slot is spent
        // when the screen is shown, which [markShown] does.
        if (!notifyInsteadOfOpening) return milestone

        // Only spend the allowance if the notification really went out.
        if (!notifier.notify(milestone)) return null

        preferencesManager.recordSharePromptShown(milestone.id)
        return milestone
    }

    /**
     * Consumes this month's slot for a milestone that has just been shown on screen.
     *
     * Recording the milestone as celebrated here is what stops it ever being raised again, which is
     * the behaviour a "strictly milestone-only, never follow up" prompt requires.
     */
    suspend fun markShown(milestoneId: String) {
        if (milestoneId.isBlank()) return
        preferencesManager.recordSharePromptShown(milestoneId)
    }
}