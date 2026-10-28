package com.example.vista.domain.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Decides whether *this* viewer should be offered a share prompt at all.
 *
 * Two independent brakes, both required:
 *
 * 1. **Segmentation.** New and casual viewers are excluded until they have actually got value from
 *    the app. Being asked to evangelise a product you have not used yet is how you get an app
 *    uninstalled, so a prompt only goes to someone who has opened it repeatedly *and* finished
 *    something.
 * 2. **Frequency cap.** A hard monthly ceiling plus a cooldown. The monthly ceiling is what stops a
 *    prolific viewer from hitting the cap and being asked again on the very next launch.
 *
 * Pure and clock-injected, so the time-relative rules are testable on any day rather than only on
 * the day they were written.
 */
object SharePromptPolicy {

    /** "Opened the app multiple times" - one launch is curiosity, three is a habit. */
    const val MIN_LAUNCHES = 3

    /** Experiencing the value means finishing something, not just tapping around. */
    const val MIN_COMPLETED_TITLES = 1

    /** Plus a bit of breadth, so the prompt does not fire on someone who watched one thing. */
    const val MIN_TITLES_STARTED = 3

    /** The user's "once or twice per month" ceiling. */
    const val MAX_PROMPTS_PER_MONTH = 2

    /** Belt-and-braces on top of the ceiling, in case month boundaries are crossed oddly. */
    const val MIN_DAYS_BETWEEN_PROMPTS = 14L

    enum class Reason {
        /** Eligible - show it. */
        ELIGIBLE,

        /** Viewer turned sharing prompts off in Settings. */
        DISABLED,

        /** Not enough launches yet, or nothing finished - a new or casual viewer. */
        NOT_POWER_USER,

        /** Already used up this month's allowance. */
        MONTHLY_CAP_REACHED,

        /** Shown too recently to ask again. */
        COOLDOWN_ACTIVE
    }

    data class Verdict(val allowed: Boolean, val reason: Reason) {
        companion object {
            val Eligible = Verdict(true, Reason.ELIGIBLE)
        }
    }

    /**
     * @param launches how many times the app has been opened on this install
     * @param titlesStarted distinct titles with any recorded progress
     * @param completedTitles titles watched to the end
     * @param promptsThisMonth prompts already delivered in the month named by [monthKey]
     * @param monthKey month the stored count belongs to, `yyyy-MM`
     * @param lastPromptAt epoch millis of the last delivered prompt, 0 if never
     */
    fun evaluate(
        enabled: Boolean,
        launches: Int,
        titlesStarted: Int,
        completedTitles: Int,
        promptsThisMonth: Int,
        monthKey: String,
        lastPromptAt: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Verdict {
        if (!enabled) return Verdict(false, Reason.DISABLED)

        val isPowerUser = launches >= MIN_LAUNCHES &&
            completedTitles >= MIN_COMPLETED_TITLES &&
            titlesStarted >= MIN_TITLES_STARTED
        if (!isPowerUser) return Verdict(false, Reason.NOT_POWER_USER)

        // A stored count from a previous month is not this month's allowance. Comparing across the
        // month boundary would otherwise keep a viewer capped forever, or let one month inherit
        // another's count.
        if (monthKey != currentMonthKey(nowMillis, zone)) {
            return if (cooledDown(lastPromptAt, nowMillis)) {
                Verdict.Eligible
            } else {
                Verdict(false, Reason.COOLDOWN_ACTIVE)
            }
        }

        if (promptsThisMonth >= MAX_PROMPTS_PER_MONTH) {
            return Verdict(false, Reason.MONTHLY_CAP_REACHED)
        }

        if (!cooledDown(lastPromptAt, nowMillis)) {
            return Verdict(false, Reason.COOLDOWN_ACTIVE)
        }

        return Verdict.Eligible
    }

    private fun cooledDown(lastPromptAt: Long, nowMillis: Long): Boolean {
        // Never prompted means no cooldown.
        if (lastPromptAt <= 0L) return true
        val elapsedDays = (nowMillis - lastPromptAt) / 86_400_000L
        return elapsedDays >= MIN_DAYS_BETWEEN_PROMPTS
    }

    fun currentMonthKey(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
        YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone)).toString()
}