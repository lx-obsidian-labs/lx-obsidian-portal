package com.example.vista.domain.model

/**
 * A genuine, locally-verifiable achievement worth telling someone about.
 *
 * Every [shareBody] states only what this device recorded. There is deliberately no leaderboard
 * wording here - no "top 10%", no "ahead of X users" - because Vista has no backend, no accounts
 * sync and no other users' data to compare against. Inventing a rank and putting it in front of
 * someone as fact would be the single most damaging thing this feature could do.
 */
data class SharingMilestone(
    /** Stable id, also the persisted "already celebrated" key. Must never change once shipped. */
    val id: String,
    val emoji: String,
    val headline: String,
    val shareBody: String
)

/**
 * Picks the one achievement worth surfacing, if any.
 *
 * Detection is *edge* triggered rather than level triggered: milestones are only offered the first
 * time their threshold is crossed. Re-celebrating the same 7-day streak every time the viewer hits
 * a new 7th day is exactly the nagging this feature is supposed to avoid, so a milestone is recorded
 * as celebrated the first time it fires and never fires again.
 */
object MilestoneEngine {

    /** Ordered by how much a viewer would actually want to tell a friend about it. */
    private val streakLevels = listOf(3, 7, 14, 30, 100)
    private val completedLevels = listOf(5, 10, 25, 50, 100)

    /**
     * Returns the highest-priority uncelebrated milestone the viewer has actually reached, or null.
     *
     * [celebrated] is the set of milestone ids already surfaced. It is passed in rather than kept
     * here so the whole decision stays a pure function of its inputs.
     */
    fun detect(stats: WatchStats, celebrated: Set<String>): SharingMilestone? {
        // Streaks are checked first: a streak is time spent actually using the product, so it says
        // far more about the app's value than a raw completion count.
        highestReached(streakLevels, stats.currentStreakDays, celebrated)
            ?.let { return streakMilestone(it) }

        highestReached(completedLevels, stats.titlesCompleted, celebrated)
            ?.let { return completedMilestone(it) }

        // A busy week is a one-off, not a repeatable ladder, so it is simply gated on never having
        // been celebrated.
        val BUSY_WEEK = "busy_week"
        if (stats.watchedThisWeek >= 3 && BUSY_WEEK !in celebrated) {
            return busyWeekMilestone(stats.watchedThisWeek)
        }

        return null
    }

    /**
     * Rebuilds a milestone from its id and *current* stats.
     *
     * A notification tap can arrive long after the milestone was detected - including on a cold
     * start - so the copy is regenerated from live figures rather than serialised into the intent.
     * That also means the number a viewer shares is the number that is true right now.
     *
     * Returns null when the id is unknown or its condition no longer holds (e.g. a streak that has
     * since lapsed), so the screen can fall back to a neutral message instead of showing a stale
     * claim.
     */
    fun resolve(id: String, stats: WatchStats): SharingMilestone? = when {
        id.startsWith("streak_") -> {
            val days = id.removePrefix("streak_").toIntOrNull()
            if (days != null && stats.currentStreakDays >= days && days in streakLevels) {
                streakMilestone(days)
            } else {
                null
            }
        }

        id.startsWith("completed_") -> {
            val titles = id.removePrefix("completed_").toIntOrNull()
            if (titles != null && stats.titlesCompleted >= titles && titles in completedLevels) {
                completedMilestone(titles)
            } else {
                null
            }
        }

        id == "busy_week" && stats.watchedThisWeek >= 3 -> busyWeekMilestone(stats.watchedThisWeek)
        else -> null
    }

    private fun busyWeekMilestone(titles: Int) = SharingMilestone(
        id = "busy_week",
        emoji = "🔥",
        headline = "$titles titles in one week",
        shareBody = "I've watched $titles different things in a week on Vista Cinema. " +
            "That's my kind of weekend - challenge me to do better."
    )

    /** The largest level the viewer has passed that has not been celebrated yet. */
    private fun highestReached(
        levels: List<Int>,
        current: Int,
        celebrated: Set<String>
    ): Int? = levels
        .filter { current >= it }
        .maxByOrNull { it }
        ?.takeIf { prefixFor(levels, it) !in celebrated }

    private fun prefixFor(levels: List<Int>, level: Int): String = prefixOf(levels) + "_" + level

    private fun prefixOf(levels: List<Int>): String =
        if (levels === streakLevels) "streak" else "completed"

    private fun streakMilestone(days: Int) = SharingMilestone(
        id = "streak_$days",
        emoji = "🔥",
        headline = "$days day streak",
        shareBody = "I've watched something on Vista Cinema $days days in a row. " +
            "Not going to let a streak die - come beat it."
    )

    private fun completedMilestone(titles: Int) = SharingMilestone(
        id = "completed_$titles",
        emoji = "🏆",
        headline = "$titles titles finished",
        shareBody = "Just finished my ${titles}th title on Vista Cinema. " +
            "The recommendations are dangerously accurate. What's your count?"
    )
}