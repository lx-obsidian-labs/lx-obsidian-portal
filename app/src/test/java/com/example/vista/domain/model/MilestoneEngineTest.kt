package com.example.vista.domain.model

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneEngineTest {

    private fun stats(
        streak: Int = 0,
        completed: Int = 0,
        thisWeek: Int = 0
    ) = WatchStats(
        currentStreakDays = streak,
        titlesCompleted = completed,
        watchedThisWeek = thisWeek
    )

    @Test
    fun `nothing reached means nothing to share`() {
        assertNull(MilestoneEngine.detect(stats(), celebrated = emptySet()))
    }

    @Test
    fun `first streak fires`() {
        val milestone = MilestoneEngine.detect(stats(streak = 3), emptySet())
        assertEquals("streak_3", milestone?.id)
        assertEquals("3 day streak", milestone?.headline)
    }

    @Test
    fun `a milestone fires exactly once`() {
        // The whole anti-nag guarantee: same stats, second time round, nothing.
        val first = MilestoneEngine.detect(stats(streak = 7), celebrated = setOf("streak_3"))
        assertEquals("streak_7", first?.id)

        val celebrated = setOf("streak_3", "streak_7")
        assertNull(MilestoneEngine.detect(stats(streak = 7), celebrated))
    }

    @Test
    fun `jump straight to the highest streak actually reached`() {
        // Someone returning after a month should not be walked through 3, then 7, then 14 one at a
        // time - they get the 14, which is the highest level they have actually passed.
        val milestone = MilestoneEngine.detect(stats(streak = 21), celebrated = setOf("streak_3"))
        assertEquals("streak_14", milestone?.id)
        assertEquals("14 day streak", milestone?.headline)
    }

    @Test
    fun `a streak exactly on a level boundary fires that level`() {
        assertEquals("streak_30", MilestoneEngine.detect(stats(streak = 30), emptySet())?.id)
        // 29 is not yet the 30 level, so it stays on 14.
        assertEquals("streak_14", MilestoneEngine.detect(stats(streak = 29), emptySet())?.id)
    }

    @Test
    fun `streak outranks completed titles`() {
        val milestone = MilestoneEngine.detect(stats(streak = 7, completed = 25), celebrated = emptySet())
        assertEquals("streak_7", milestone?.id)
    }

    @Test
    fun `completed count fires when there is no streak`() {
        val milestone = MilestoneEngine.detect(stats(completed = 10), celebrated = emptySet())
        assertEquals("completed_10", milestone?.id)
    }

    @Test
    fun `busy week fires once then never again`() {
        val first = MilestoneEngine.detect(stats(thisWeek = 4), celebrated = emptySet())
        assertEquals("busy_week", first?.id)

        // Even with a much busier week afterwards, having celebrated it once is enough.
        assertNull(MilestoneEngine.detect(stats(thisWeek = 9), celebrated = setOf("busy_week")))
    }

    @Test
    fun `busy week needs three titles not one`() {
        assertNull(MilestoneEngine.detect(stats(thisWeek = 2), celebrated = emptySet()))
    }

    @Test
    fun `a celebrated streak does not hide an uncelebrated completion`() {
        // Otherwise the viewer would be locked out forever by an unrelated milestone.
        val milestone = MilestoneEngine.detect(
            stats(streak = 7, completed = 5),
            celebrated = setOf("streak_3", "streak_7")
        )
        assertEquals("completed_5", milestone?.id)
    }

    @Test
    fun `copy never claims a global ranking`() {
        // Guards the honesty rule: no fabricated percentile or peer comparison anywhere in copy.
        val bodies = buildList {
            listOf(3, 7, 14, 30, 100).forEach {
                add(MilestoneEngine.detect(WatchStats(currentStreakDays = it), emptySet())!!.shareBody)
            }
            listOf(5, 10, 25, 50, 100).forEach {
                add(MilestoneEngine.detect(WatchStats(titlesCompleted = it), emptySet())!!.shareBody)
            }
            add(MilestoneEngine.detect(WatchStats(watchedThisWeek = 5), emptySet())!!.shareBody)
        }
        val banned = listOf("top %", "top 10", "ahead of", "better than", "percentile", "of users", "users have")
        bodies.forEach { body ->
            banned.forEach { phrase ->
                assertTrue("copy must not claim '$phrase': $body", !body.contains(phrase, ignoreCase = true))
            }
        }
    }
}