package com.example.vista.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePromptPolicyTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 6, 15)
    private val now: Long = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val monthKey: String = "2026-06"

    private fun evaluate(
        enabled: Boolean = true,
        launches: Int = 10,
        titlesStarted: Int = 20,
        completedTitles: Int = 12,
        promptsThisMonth: Int = 0,
        monthKey: String = this.monthKey,
        lastPromptAt: Long = 0L,
        nowMillis: Long = now
    ) = SharePromptPolicy.evaluate(
        enabled, launches, titlesStarted, completedTitles,
        promptsThisMonth, monthKey, lastPromptAt, nowMillis, zone
    )

    private fun daysAgo(days: Long) = today.minusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `power user with headroom is eligible`() {
        assertEquals(SharePromptPolicy.Reason.ELIGIBLE, evaluate().reason)
    }

    @Test
    fun `disabled in settings wins over everything`() {
        // Even a fully eligible viewer must never be prompted once they have opted out.
        assertEquals(
            SharePromptPolicy.Reason.DISABLED,
            evaluate(enabled = false).reason
        )
    }

    @Test
    fun `brand new user is excluded`() {
        assertEquals(
            SharePromptPolicy.Reason.NOT_POWER_USER,
            evaluate(launches = 1).reason
        )
    }

    @Test
    fun `casual user who never finished anything is excluded`() {
        // Opened plenty of times, watched stuff, finished nothing - has not got value yet.
        assertEquals(
            SharePromptPolicy.Reason.NOT_POWER_USER,
            evaluate(completedTitles = 0).reason
        )
    }

    @Test
    fun `user with a single title is excluded`() {
        assertEquals(
            SharePromptPolicy.Reason.NOT_POWER_USER,
            evaluate(titlesStarted = 1, completedTitles = 1).reason
        )
    }

    @Test
    fun `exactly at the launch threshold is allowed`() {
        assertTrue(
            evaluate(
                launches = SharePromptPolicy.MIN_LAUNCHES,
                titlesStarted = SharePromptPolicy.MIN_TITLES_STARTED,
                completedTitles = SharePromptPolicy.MIN_COMPLETED_TITLES
            ).allowed
        )
    }

    @Test
    fun `one below the launch threshold is excluded`() {
        assertFalse(
            evaluate(
                launches = SharePromptPolicy.MIN_LAUNCHES - 1,
                titlesStarted = SharePromptPolicy.MIN_TITLES_STARTED,
                completedTitles = SharePromptPolicy.MIN_COMPLETED_TITLES
            ).allowed
        )
    }

    @Test
    fun `monthly cap of two is enforced`() {
        assertEquals(
            SharePromptPolicy.Reason.MONTHLY_CAP_REACHED,
            evaluate(promptsThisMonth = SharePromptPolicy.MAX_PROMPTS_PER_MONTH).reason
        )
        assertEquals(
            SharePromptPolicy.Reason.MONTHLY_CAP_REACHED,
            evaluate(promptsThisMonth = 5).reason
        )
    }

    @Test
    fun `second prompt inside the month is still allowed`() {
        assertTrue(
            evaluate(
                promptsThisMonth = SharePromptPolicy.MAX_PROMPTS_PER_MONTH - 1,
                // Outside the cooldown, which is what actually gates the second prompt.
                lastPromptAt = daysAgo(SharePromptPolicy.MIN_DAYS_BETWEEN_PROMPTS + 1)
            ).allowed
        )
    }

    @Test
    fun `cooldown blocks a prompt too soon after the last one`() {
        assertEquals(
            SharePromptPolicy.Reason.COOLDOWN_ACTIVE,
            evaluate(lastPromptAt = daysAgo(3)).reason
        )
    }

    @Test
    fun `cooldown boundary is inclusive`() {
        assertTrue(
            evaluate(lastPromptAt = daysAgo(SharePromptPolicy.MIN_DAYS_BETWEEN_PROMPTS)).allowed
        )
    }

    @Test
    fun `count from a previous month does not consume this month`() {
        // Otherwise a viewer capped in May is still capped in June on the first eligible day.
        assertTrue(
            evaluate(
                promptsThisMonth = 2,
                monthKey = "2026-05",
                lastPromptAt = daysAgo(SharePromptPolicy.MIN_DAYS_BETWEEN_PROMPTS + 1)
            ).allowed
        )
    }

    @Test
    fun `previous month count still respects the cooldown`() {
        assertEquals(
            SharePromptPolicy.Reason.COOLDOWN_ACTIVE,
            evaluate(monthKey = "2026-05", lastPromptAt = daysAgo(2)).reason
        )
    }

    @Test
    fun `never prompted means no cooldown`() {
        assertTrue(evaluate(lastPromptAt = 0L).allowed)
    }

    @Test
    fun `month key matches the supplied clock`() {
        assertEquals("2026-06", SharePromptPolicy.currentMonthKey(now, zone))
        // Pinned dates rather than an offset, so the expected value is obvious on review.
        val january2027 = LocalDate.of(2027, 1, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("2027-01", SharePromptPolicy.currentMonthKey(january2027, zone))
        // A month boundary falls on a different instant per zone, so the key must come from the
        // caller's zone: 00:30 on 1 Jan in Tokyo is still 31 Dec in UTC.
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val boundary = LocalDate.of(2027, 1, 1).atStartOfDay(tokyoZone).plusMinutes(30)
            .toInstant().toEpochMilli()
        assertEquals("2027-01", SharePromptPolicy.currentMonthKey(boundary, tokyoZone))
        assertEquals("2026-12", SharePromptPolicy.currentMonthKey(boundary, zone))
    }
}